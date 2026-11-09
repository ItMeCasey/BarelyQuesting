package com.itmecasey.barelyquesting.dev;

import com.itmecasey.barelyquesting.compat.ClientHooks;
import com.itmecasey.barelyquesting.compat.McGui;
import com.itmecasey.barelyquesting.compat.QuestScreen;
import com.itmecasey.barelyquesting.theme.ThemeManager;
import com.itmecasey.barelyquesting.ui.BookUi;
import com.itmecasey.barelyquesting.ui.ClientState;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Development-only client tour (-PdevTools, env BARELYQUESTING_TOUR). Makes a flat world, opens the book, presses its buttons through
 * the screen's own handlers (never the OS cursor), saves screenshots into run/ and quits. Progress goes to the log as BQ-TOUR lines.
 */
public final class Tour {
    private static Minecraft mc;

    private Tour() {
    }

    public static void start() {
        Thread t = new Thread(Tour::run, "barelyquesting-tour");
        t.setDaemon(true);
        t.start();
    }

    private static void log(String s) {
        System.out.println("BQ-TOUR " + s);
    }

    private static void run() {
        try {
            while ((mc = Minecraft.getInstance()) == null) {
                Thread.sleep(200);
            }
            while (mc.level != null || McGui.screen() == null) {
                Thread.sleep(500);
            }
            Thread.sleep(5000);
            log("creating a fresh flat world");
            onClient(() -> {
                LevelSettings settings = new LevelSettings("bq_tour", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                        new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("bq_tour", settings, new WorldOptions(0L, false, false),
                        registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            });
            while (!(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null && McGui.screen() == null)) {
                Thread.sleep(500);
            }
            log("in the world");
            Thread.sleep(6000);
            tour();
            log("DONE");
        } catch (Throwable t) {
            log("FAILED " + t);
            t.printStackTrace();
        } finally {
            if (mc != null) {
                mc.execute(() -> mc.stop());
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
                // going down anyway
            }
            Runtime.getRuntime().halt(0);
        }
    }

    private static BookUi ui() {
        return ((QuestScreen) McGui.screen()).ui();
    }

    private static void press(String label) throws Exception {
        pressed(label);
    }

    private static boolean pressed(String label) throws Exception {
        boolean[] ok = {false};
        onClient(() -> ok[0] = ui().press(label));
        log("press " + label + " -> " + ok[0]);
        Thread.sleep(700);
        return ok[0];
    }

    private static void tour() throws Exception {
        MinecraftServer server = mc.getSingleplayerServer();
        ClientState st = ClientState.get();
        log("connected=" + st.connected + " canEdit=" + st.canEdit + " chapters=" + st.book.chapters.size());


        onClient(() -> ClientHooks.openBook(null));
        Thread.sleep(2500);
        shot("graph");
        onClient(() -> ui().select("bench"));
        Thread.sleep(700);
        shot("graph_selected");

        press("List");
        shot("list");

        press("Graph");
        press("Themes");
        shot("themes");
        onClient(() -> ui().closeModal());
        Thread.sleep(300);
        onClient(() -> ThemeManager.select("barelyquesting:parchment"));
        Thread.sleep(700);
        shot("parchment_graph");
        press("List");
        shot("parchment_list");
        press("Graph");

        press("Edit");
        shot("edit");
        onClient(() -> ui().select("wood"));
        Thread.sleep(700);
        shot("edit_quest");
        press("+ Quest");
        Thread.sleep(900);
        shot("edit_added");
        press("Undo");
        Thread.sleep(500);
        press("Team");
        shot("team");
        onClient(() -> ui().closeModal());
        Thread.sleep(300);
        onClient(() -> ThemeManager.select("barelyquesting:default"));

        // finish a quest from the server side and look at the toast and the claim button (edit mode swaps the player panel for the editor, so leave it first)
        press("Edit");
        onClient(() -> ui().select("wood"));
        command("barelyquesting complete @p wood");
        Thread.sleep(1500);
        shot("claim_toast");
        if (!pressed("Claim all")) throw new IllegalStateException("Claim all button missing for a completed quest");
        Thread.sleep(700);
        shot("claimed");
        onClient(() -> McGui.setScreen(null));
        onClient(() -> st.pinned.add("pickaxe"));
        command("barelyquesting complete @p bench");
        Thread.sleep(1500);
        shot("hud");
        // the viewer shortcut's lookup: open the quest that crafts a wooden pickaxe
        onClient(() -> {
            if (!com.itmecasey.barelyquesting.compat.ItemQuestKey.openForItem("minecraft:wooden_pickaxe")) {
                throw new IllegalStateException("no quest found for the wooden pickaxe");
            }
        });
        Thread.sleep(900);
        shot("item_quest");
    }

    private static void command(String text) throws Exception {
        MinecraftServer server = mc.getSingleplayerServer();
        CompletableFuture<Void> done = new CompletableFuture<>();
        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(server.getPlayerList().getPlayers().get(0).createCommandSourceStack(), text);
            } finally {
                done.complete(null);
            }
        });
        done.get(10, TimeUnit.SECONDS);
        Thread.sleep(100);
    }

    private static void onClient(Runnable r) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        mc.execute(() -> {
            try {
                r.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        done.get(15, TimeUnit.SECONDS);
    }

    private static void shot(String name) throws Exception {
        log("shot " + name);
        CompletableFuture<Void> done = new CompletableFuture<>();
        String file = "bq_" + name + ".png";
        onClient(() -> {
            try {
                Object target = mc.getMainRenderTarget();
                for (Method m : Class.forName("net.minecraft.client.Screenshot").getMethods()) {
                    if (!m.getName().equals("grab") || !Modifier.isStatic(m.getModifiers())) {
                        continue;
                    }
                    Class<?>[] p = m.getParameterTypes();
                    Consumer<Object> after = msg -> done.complete(null);
                    if (p.length == 4) {
                        m.invoke(null, mc.gameDirectory, file, target, after);
                    } else if (p.length == 5) {
                        m.invoke(null, mc.gameDirectory, file, target, 1, after);
                    } else {
                        continue;
                    }
                    return;
                }
                throw new IllegalStateException("no Screenshot.grab");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        done.get(15, TimeUnit.SECONDS);
        log("saved " + new File(mc.gameDirectory, "screenshots/" + file));
    }
}

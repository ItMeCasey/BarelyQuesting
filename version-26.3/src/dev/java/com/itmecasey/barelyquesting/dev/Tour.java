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
import net.minecraft.client.gui.screens.TitleScreen;
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
    static Minecraft mc;

    private Tour() {
    }

    public static void start() {
        Thread t = new Thread(Tour::run, "barelyquesting-tour");
        t.setDaemon(true);
        t.start();
    }

    static boolean normalWorld() {
        String only = System.getenv("BARELYQUESTING_TASKS_ONLY");
        return only != null && !only.equals("jkey");
    }

    static void log(String s) {
        System.out.println("BQ-TOUR " + s);
    }

    private static void run() {
        try {
            while ((mc = Minecraft.getInstance()) == null) {
                Thread.sleep(200);
            }
            while (mc.gui == null || mc.level != null || McGui.screen() == null) {
                Thread.sleep(500);
            }
            Thread.sleep(5000);
            log("creating a fresh flat world");
            onClient(() -> {
                LevelSettings settings = new LevelSettings("bq_tour", GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("bq_tour", settings, new WorldOptions(0L, normalWorld(), false),
                        registries -> registries.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow((normalWorld() ? WorldPresets.NORMAL : WorldPresets.FLAT)).value().createWorldDimensions(), new TitleScreen());
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

    static BookUi ui() {
        return ((QuestScreen) McGui.screen()).ui();
    }

    static void press(String label) throws Exception {
        pressed(label);
    }

    static boolean pressed(String label) throws Exception {
        boolean[] ok = {false};
        onClient(() -> ok[0] = ui().press(label));
        log("press " + label + " -> " + ok[0]);
        Thread.sleep(700);
        return ok[0];
    }

    private static void tour() throws Exception {
        MinecraftServer server = mc.getSingleplayerServer();
        ClientState st = ClientState.get();
        if (System.getenv("BARELYQUESTING_TASKS_ONLY") != null) {
            Tour2.tasksOnly();
            return;
        }
        log("connected=" + st.connected + " canEdit=" + st.canEdit + " chapters=" + st.book.chapters.size());

        if (System.getenv("BARELYQUESTING_INTEGRATIONS") != null) {
            for (String mod : System.getenv("BARELYQUESTING_INTEGRATIONS").split(",")) {
                if (st.book.quest(mod + "_quest") == null) throw new IllegalStateException(mod + " startup quest missing");
                CompletableFuture<Void> checked = new CompletableFuture<>();
                server.execute(() -> {
                    try {
                        var engine = com.itmecasey.barelyquesting.compat.ServerHooks.quests().engine();
                        var player = server.getPlayerList().getPlayers().get(0);
                        var team = engine.teams().teamOf(player.getUUID()).id;
                        engine.reset(team, mod + "_quest");
                        engine.poll(new com.itmecasey.barelyquesting.compat.McPlayer(player));
                        com.itmecasey.barelyquesting.compat.ServerHooks.quests().flush();
                        if (!engine.isComplete(team, mod + "_quest")) throw new IllegalStateException(mod + " callback did not complete task");
                        checked.complete(null);
                    } catch (Throwable t) { checked.completeExceptionally(t); }
                });
                checked.get(10, TimeUnit.SECONDS);
                Thread.sleep(500);
                onClient(() -> {
                    var progress = st.progress.get(st.teamId, mod + "_quest");
                    if (progress == null || !progress.complete || progress.task("ready") != 1)
                        throw new IllegalStateException(mod + " fresh callback progress did not sync");
                });
                log("INTEGRATION PASS " + mod + " startup creation, fresh callback progress and client sync");
            }
        }
        onClient(() -> ClientHooks.openBook(null));
        Thread.sleep(2500);
        shot("graph");
        if (System.getenv("BARELYQUESTING_INTEGRATIONS") != null) {
            for (String mod : System.getenv("BARELYQUESTING_INTEGRATIONS").split(",")) {
                onClient(() -> { ui().chapterId = mod + "_validation"; ui().select(mod + "_quest"); });
                Thread.sleep(600);
                shot(mod + "_integration");
            }
        }
        if ("none".equals(System.getenv("BARELYQUESTING_VIEWER"))) {
            onClient(() -> {
                var before = McGui.screen();
                if (!com.itmecasey.barelyquesting.compat.Viewers.installed().isEmpty()
                        || com.itmecasey.barelyquesting.compat.Viewers.show(com.itmecasey.barelyquesting.core.model.ItemSpec.of("minecraft:oak_planks"), false)
                        || McGui.screen() != before) throw new IllegalStateException("no-viewer fallback changed the screen");
            });
            log("INTEGRATION PASS no optional viewers; quest screen remains usable");
        } else if (System.getenv("BARELYQUESTING_VIEWER") != null) {
            for (boolean uses : new boolean[] {false, true}) {
                boolean[] opened = {false};
                onClient(() -> opened[0] = com.itmecasey.barelyquesting.compat.Viewers.show(
                        com.itmecasey.barelyquesting.core.model.ItemSpec.of("minecraft:oak_planks"), uses));
                if (!opened[0]) throw new IllegalStateException("viewer lookup failed: uses=" + uses);
                Thread.sleep(1500);
                shot("viewer_" + (uses ? "uses" : "recipes"));
                log("INTEGRATION PASS viewer " + (uses ? "uses" : "recipes"));
                if ("bei".equals(System.getenv("BARELYQUESTING_VIEWER"))) {
                    onClient(() -> {
                        try {
                            Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("close").invoke(null);
                        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                        McGui.screen().tick();
                        if (!(McGui.screen() instanceof QuestScreen)) throw new IllegalStateException("BEI did not return to the quest screen");
                        try {
                            if ((Boolean) Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("isOpen").invoke(null))
                                throw new IllegalStateException("BEI left a stale popup");
                        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                    });
                    log("INTEGRATION PASS BEI return and popup cleanup");
                }
                onClient(() -> ClientHooks.openBook(null));
                Thread.sleep(500);
            }
        }

        if (System.getenv("BARELYQUESTING_INTEGRATION_ONLY") != null) return;

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
        boolean claimed = false;
        for (int attempt = 0; attempt < 12 && !claimed; attempt++) {
            claimed = pressed("Claim all");
            if (!claimed) {
                Thread.sleep(700);
            }
        }
        if (!claimed) {
            throw new IllegalStateException("Claim all button missing for a completed quest");
        }
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
        Tour2.run();
    }

    static void command(String text) throws Exception {
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

    static void onClient(Runnable r) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        mc.execute(() -> {
            try {
                r.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        done.get(60, TimeUnit.SECONDS);
    }

    static void shot(String name) throws Exception {
        log("shot " + name);
        CompletableFuture<Void> done = new CompletableFuture<>();
        String file = "bq_" + name + ".png";
        onClient(() -> {
            try {
                Object target = mc.gameRenderer.mainRenderTarget();
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
        done.get(60, TimeUnit.SECONDS);
        log("saved " + new File(mc.gameDirectory, "screenshots/" + file));
    }
}

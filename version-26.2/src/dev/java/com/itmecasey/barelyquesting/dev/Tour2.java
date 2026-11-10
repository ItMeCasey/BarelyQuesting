package com.itmecasey.barelyquesting.dev;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.compat.ClientHooks;
import com.itmecasey.barelyquesting.compat.ItemQuestKey;
import com.itmecasey.barelyquesting.compat.Mc;
import com.itmecasey.barelyquesting.compat.McGui;
import com.itmecasey.barelyquesting.compat.QuestScreen;
import com.itmecasey.barelyquesting.compat.ServerHooks;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.theme.ThemeManager;
import com.itmecasey.barelyquesting.ui.ClientState;
import com.itmecasey.barelyquesting.ui.Keys;
import com.itmecasey.barelyquesting.ui.Modals;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;

/**
 * Second client tour: the checks the first tour did not make. Everything goes through the game's own handlers (the book screen's key and
 * char handlers, MouseHandler.onMove, the game mode's useItem, the resource reload F3+T runs, commands), never the OS keyboard or cursor.
 * Each check logs "BQ-TOUR CHECK name PASS" or "FAIL reason"; any failure makes the tour fail at the end.
 */
final class Tour2 {
    private static Minecraft mc;
    private static final List<String> FAILED = new ArrayList<>();

    private interface Check {
        void run() throws Exception;
    }

    private Tour2() {
    }

    private static void check(String name, Check c) {
        try {
            c.run();
            Tour.log("CHECK " + name + " PASS");
        } catch (Throwable t) {
            FAILED.add(name);
            Tour.log("CHECK " + name + " FAIL " + t);
        }
    }

    private static void waitFor(String what, BooleanSupplier cond) throws Exception {
        for (int i = 0; i < 80; i++) {
            if (cond.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("timed out waiting for " + what);
    }

    private static QuestScreen screen() {
        return McGui.screen() instanceof QuestScreen q ? q : null;
    }

    private static ServerPlayer player() {
        return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static <T> T onServer(java.util.concurrent.Callable<T> job) throws Exception {
        return onServer(job, 10);
    }

    private static <T> T onServer(java.util.concurrent.Callable<T> job, int seconds) throws Exception {
        CompletableFuture<T> done = new CompletableFuture<>();
        mc.getSingleplayerServer().execute(() -> {
            try {
                done.complete(job.call());
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        return done.get(seconds, TimeUnit.SECONDS);
    }

    /** A command whose work (chunk generation, dimension change) can take longer than the tour's usual 10 seconds. */
    private static void slow(String command) throws Exception {
        Tour.log("running " + command);
        locate(command);
    }

    private static void openBook() throws Exception {
        Tour.onClient(() -> ClientHooks.openBook(null));
        waitFor("book screen", () -> screen() != null);
        Thread.sleep(400);
    }

    private static void type(String text) throws Exception {
        for (char c : text.toCharArray()) {
            Tour.onClient(() -> screen().typeChar(c));
        }
    }

    /** Only the live-world task checks, so one launch stays inside the time limit. */
    static void tasksOnly() throws Exception {
        mc = Minecraft.getInstance();
        if ("jkey".equals(System.getenv("BARELYQUESTING_TASKS_ONLY"))) {
            check("j-keybind-opens-the-book", () -> {
                Tour.onClient(() -> McGui.setScreen(null));
                Thread.sleep(500);
                // KeyMapping.click is what the keyboard handler calls for a pressed key; the book's own client tick then consumes it
                Tour.onClient(() -> net.minecraft.client.KeyMapping.click(ClientHooks.openKey().getKey()));
                waitFor("book opened by the J keybind", () -> screen() != null);
                Thread.sleep(400);
                Tour.shot("j_key");
                Tour.onClient(() -> McGui.setScreen(null));
            });
        } else {
            check("tasks-in-a-live-world", Tour2::tasks);
        }
        if (!FAILED.isEmpty()) {
            throw new IllegalStateException("failed checks: " + FAILED);
        }
    }

    static void run() throws Exception {
        mc = Minecraft.getInstance();
        ClientState st = ClientState.get();

        check("f3t-reload", () -> {
            for (String theme : new String[] {"barelyquesting:parchment", "barelyquesting:default"}) {
                Tour.onClient(() -> {
                    ThemeManager.select(theme);
                    st.saveSettings();
                });
                int before = ClientHooks.reloads;
                Tour.onClient(() -> mc.reloadResourcePacks());
                waitFor("resource reload", () -> ClientHooks.reloads > before);
                if (!theme.equals(ThemeManager.selectedId())) {
                    throw new IllegalStateException("theme " + theme + " became " + ThemeManager.selectedId() + " after the reload");
                }
                Thread.sleep(800);
                Tour.shot("reload_" + theme.substring(theme.indexOf(':') + 1));
            }
        });

        check("editor-undo-redo", () -> {
            openBook();
            if (!Tour.ui().editing) {
                Tour.press("Edit");
            }
            int n = st.book.allQuests().size();
            Tour.press("+ Quest");
            waitFor("quest added", () -> st.book.allQuests().size() == n + 1);
            Tour.press("Undo");
            waitFor("undo", () -> st.book.allQuests().size() == n);
            Tour.press("Redo");
            waitFor("redo", () -> st.book.allQuests().size() == n + 1);
            Tour.press("Undo");
            waitFor("second undo", () -> st.book.allQuests().size() == n);
            Tour.press("Edit");
            if (Tour.ui().editing) {
                throw new IllegalStateException("edit mode did not switch off");
            }
        });

        check("editor-item-picker", () -> {
            openBook();
            ItemSpec[] picked = new ItemSpec[1];
            Tour.onClient(() -> Tour.ui().open(new Modals.ItemPick(ItemSpec.of("minecraft:book"), false, s -> picked[0] = s)));
            type("wooden_pickaxe");
            Thread.sleep(700);
            Tour.shot("picker");
            Tour.onClient(() -> screen().pressKey(Keys.ENTER, 0));
            waitFor("picked item", () -> picked[0] != null);
            if (!picked[0].id().equals("minecraft:wooden_pickaxe")) {
                throw new IllegalStateException("picked " + picked[0].id());
            }
        });

        check("commands", () -> {
            int quests = st.book.allQuests().size();
            Tour.command("barelyquesting reload");
            Tour.command("barelyquesting reset @p wood");
            var q = ServerHooks.quests();
            UUID me = player().getUUID();
            if (onServer(() -> q.engine().isComplete(q.engine().teams().teamOf(me).id, "wood"))) {
                throw new IllegalStateException("reset did not clear wood");
            }
            Tour.command("barelyquesting complete @p wood");
            if (!onServer(() -> q.engine().isComplete(q.engine().teams().teamOf(me).id, "wood"))) {
                throw new IllegalStateException("complete did not finish wood");
            }
            Tour.onClient(() -> McGui.setScreen(null));
            Tour.command("barelyquesting open bench");
            waitFor("open <quest>", () -> screen() != null && "bench".equals(screen().ui().selected));
            Tour.onClient(() -> McGui.setScreen(null));
            Tour.command("bq edit");
            waitFor("edit", () -> screen() != null && screen().ui().editing);
            if (st.book.allQuests().size() < quests) {
                throw new IllegalStateException("reload lost quests");
            }
            Tour.press("Edit");
        });

        check("hud-and-toast", () -> {
            Tour.onClient(() -> McGui.setScreen(null));
            Tour.onClient(() -> {
                st.pinned.clear();
                st.pinned.add("pickaxe");
                st.hud = true;
            });
            Tour.command("barelyquesting reset @p pickaxe");
            Tour.command("barelyquesting reset @p bench");
            Tour.command("barelyquesting complete @p bench");
            waitFor("toast queued", () -> !st.toasts.isEmpty());
            Thread.sleep(600);
            Tour.shot("hud2");
        });

        check("quest-book-item", () -> {
            Tour.onClient(() -> McGui.setScreen(null));
            Tour.command("item replace entity @p weapon.mainhand with barelyquesting:quest_book");
            Thread.sleep(500);
            Tour.onClient(() -> {
                if (!mc.player.getMainHandItem().getItem().toString().contains("quest_book")) {
                    throw new IllegalStateException("held " + mc.player.getMainHandItem().getItem());
                }
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            });
            waitFor("book opened by the item", () -> screen() != null);
            Thread.sleep(500);
            Tour.shot("quest_book_item");
            Tour.onClient(() -> McGui.setScreen(null));
        });

        check("ctrl-j-over-inventory-slot", () -> {
            Tour.command("gamemode survival @p");
            Tour.command("item replace entity @p hotbar.0 with minecraft:crafting_table");
            Thread.sleep(400);
            Tour.onClient(() -> McGui.setScreen(new InventoryScreen(mc.player)));
            Thread.sleep(600);
            int[] at = new int[2];
            Tour.onClient(() -> {
                try {
                    AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) McGui.screen();
                    Field left = AbstractContainerScreen.class.getDeclaredField("leftPos");
                    Field top = AbstractContainerScreen.class.getDeclaredField("topPos");
                    left.setAccessible(true);
                    top.setAccessible(true);
                    for (Slot slot : mc.player.inventoryMenu.slots) {
                        if (slot.getItem().getItem().toString().contains("crafting_table")) {
                            double gx = left.getInt(screen) + slot.x + 8;
                            double gy = top.getInt(screen) + slot.y + 8;
                            at[0] = (int) (gx * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth());
                            at[1] = (int) (gy * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight());
                            return;
                        }
                    }
                    throw new IllegalStateException("crafting table not in the inventory");
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            });
            moveMouse(at[0], at[1]);
            Thread.sleep(700);
            String[] hovered = new String[1];
            Tour.onClient(() -> hovered[0] = ItemQuestKey.hoveredItemId());
            if (!"minecraft:crafting_table".equals(hovered[0])) {
                String[] diag = new String[1];
                Tour.onClient(() -> {
                    try {
                        Field hs = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
                        hs.setAccessible(true);
                        diag[0] = "mouse " + mc.mouseHandler.xpos() + "," + mc.mouseHandler.ypos() + " wanted " + at[0] + "," + at[1]
                                + " screen " + McGui.screen() + " hoveredSlot " + hs.get(McGui.screen());
                    } catch (ReflectiveOperationException e) {
                        diag[0] = e.toString();
                    }
                });
                throw new IllegalStateException("hovered " + hovered[0] + " (" + diag[0] + ")");
            }
            McGui.forcedKeys.add(com.mojang.blaze3d.platform.InputConstants.KEY_J);
            McGui.forcedKeys.add(com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL);
            try {
                waitFor("Ctrl+J to open the quest", () -> screen() != null && "bench".equals(screen().ui().selected));
            } finally {
                McGui.forcedKeys.clear();
            }
            Thread.sleep(500);
            Tour.shot("ctrl_j");
            Tour.onClient(() -> McGui.setScreen(null));
            Tour.command("gamemode creative @p");
        });

        check("teams", () -> {
            openBook();
            Tour.press("Team");
            Tour.press("Create team...");
            type("Crew");
            Tour.shot("team_create");
            Tour.press("OK");
            var q = ServerHooks.quests();
            UUID me = player().getUUID();
            waitFor("team created", () -> {
                try {
                    return !onServer(() -> q.engine().teams().teamOf(me).solo);
                } catch (Exception e) {
                    return false;
                }
            });
            String teamId = onServer(() -> q.engine().teams().teamOf(me).id);
            Thread.sleep(600);
            Tour.press("Team");
            UUID second = UUID.fromString("00000000-0000-0000-0000-0000000000f4");
            String invited = onServer(() -> q.engine().teams().invite(me, second));
            String joined = onServer(() -> q.engine().teams().join(second, teamId));
            if (invited != null || joined != null) {
                throw new IllegalStateException("invite " + invited + ", join " + joined);
            }
            onServer(() -> {
                q.flush();
                return null;
            });
            waitFor("client sees two members", () -> st.team.toString().contains(second.toString()));
            Thread.sleep(500);
            Tour.shot("team_two_members");
            Tour.command("barelyquesting reset @p wood");
            Tour.command("barelyquesting complete @p wood");
            if (onServer(() -> q.engine().status(teamId, second, q.engine().book().quest("wood"))) != QuestEngine.Status.COMPLETE
                    && onServer(() -> q.engine().status(teamId, second, q.engine().book().quest("wood"))) != QuestEngine.Status.CLAIMED) {
                throw new IllegalStateException("the second member does not see the shared completion");
            }
            Tour.press("Disband team");
            Thread.sleep(400);
            Tour.press("Yes");
            waitFor("owner back to a solo team", () -> {
                try {
                    return onServer(() -> q.engine().teams().teamOf(me).solo && q.engine().teams().teamOf(second).solo);
                } catch (Exception e) {
                    return false;
                }
            });
            Tour.shot("team_left");
            Tour.onClient(() -> McGui.setScreen(null));
        });

        if (System.getenv("BARELYQUESTING_TASKS_ONLY") == null) {
            Tour.log("tasks run in their own launch (BARELYQUESTING_TASKS_ONLY)");
        }

        if (!FAILED.isEmpty()) {
            throw new IllegalStateException("failed checks: " + FAILED);
        }
    }

    private static void moveMouse(double windowX, double windowY) throws Exception {
        Tour.onClient(() -> {
            try {
                Object handler = mc.mouseHandler;
                Field x = handler.getClass().getDeclaredField("xpos");
                Field y = handler.getClass().getDeclaredField("ypos");
                x.setAccessible(true);
                y.setAccessible(true);
                x.setDouble(handler, windowX);
                y.setDouble(handler, windowY);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static boolean hasMethod(Object o, String name) {
        for (Method m : o.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 0) {
                return true;
            }
        }
        return false;
    }

    /** Reads the hovered item out of whatever viewer overlay is installed by sweeping the mouse over the inventory screen. */
    static void hover(String viewer) throws Exception {
        mc = Minecraft.getInstance();
        Tour.command("gamemode survival @p");
        Tour.command("clear @p");
        Tour.onClient(() -> McGui.setScreen(new InventoryScreen(mc.player)));
        Thread.sleep(1500);
        int w = mc.getWindow().getScreenWidth();
        int h = mc.getWindow().getScreenHeight();
        String found = null;
        int[] where = new int[2];
        for (int xi = 3; xi <= 19 && found == null; xi++) {
            for (int yi = 2; yi <= 18 && found == null; yi++) {
                int x = w * xi / 20;
                int y = h * yi / 20;
                moveMouse(x, y);
                Thread.sleep(40);
                String[] id = new String[1];
                Tour.onClient(() -> id[0] = ItemQuestKey.hoveredItemId());
                if (id[0] != null) {
                    found = id[0];
                    where[0] = x;
                    where[1] = y;
                }
            }
        }
        Tour.shot("hover_" + viewer);
        if (found == null) {
            throw new IllegalStateException("no item read from " + viewer + " anywhere over the inventory screen");
        }
        Tour.log("HOVER " + viewer + " read " + found + " at window " + where[0] + "," + where[1]);
        Tour.log("CHECK hover-" + viewer + " PASS");
    }

    private static void tasks() throws Exception {
        Path dir = Mc.configDir.resolve("barelyquesting").resolve("chapters");
        String spawnBiome = onServer(() -> {
            var level = player().level();
            return level.getBiome(player().blockPosition()).unwrapKey().get().toString();
        });
        spawnBiome = spawnBiome.substring(spawnBiome.lastIndexOf(" / ") + 3, spawnBiome.length() - 1);
        Tour.log("spawn biome " + spawnBiome);
        double[] start = onServer(() -> new double[] {player().getX(), player().getY(), player().getZ()});
        String json = "{\"id\":\"tour_tasks\",\"title\":\"Tour tasks\",\"icon\":\"minecraft:map\",\"quests\":["
                + quest("t_loc", 0, "{\"id\":\"t0\",\"type\":\"location\",\"dimension\":\"minecraft:overworld\",\"x\":120,\"y\":250,\"z\":120,\"radius\":60}")
                + "," + quest("t_dim", 2, "{\"id\":\"t0\",\"type\":\"dimension\",\"dimension\":\"minecraft:the_nether\"}")
                + "," + quest("t_biome", 4, "{\"id\":\"t0\",\"type\":\"biome\",\"biome\":\"" + spawnBiome + "\"}")
                + "," + quest("t_struct", 6, "{\"id\":\"t0\",\"type\":\"structure\",\"structure\":\"minecraft:village_plains\"}")
                + "," + quest("t_check", 8, "{\"id\":\"t0\",\"type\":\"checkmark\",\"text\":\"Tick me\"}")
                + "]}";
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("tour_tasks.json"), json, StandardCharsets.UTF_8);
        Tour.command("barelyquesting reload");
        ClientState st = ClientState.get();
        waitFor("tour chapter on the client", () -> st.book.quest("t_loc") != null);
        var q = ServerHooks.quests();
        UUID me = player().getUUID();
        String team = onServer(() -> q.engine().teams().teamOf(me).id);

        boolean structureOnly = "structure".equals(System.getenv("BARELYQUESTING_TASKS_ONLY"));
        slow("gamemode creative @p");
        if (!structureOnly) {
            slow("tp @p 120 250 120");
            waitFor("location task", () -> done(q, team, "t_loc"));
            slow("execute in minecraft:the_nether run tp @p 0 120 0");
            waitFor("dimension task", () -> done(q, team, "t_dim"));
            slow("execute in minecraft:overworld run tp @p " + start[0] + " " + start[1] + " " + start[2]);
            waitFor("biome task", () -> done(q, team, "t_biome"));
        }
        if (structureOnly) {
        long t0 = System.currentTimeMillis();
        Object[] found = onServer(Tour2::nearestStructure, 100);
        Tour.log("nearest structure search took " + (System.currentTimeMillis() - t0) + " ms: " + (found == null ? "none" : found[2] + " at " + found[0] + "," + found[1]));
        if (found == null) {
            throw new IllegalStateException("no structure generated in the four regions around the spawn");
        }
        Files.writeString(dir.resolve("tour_tasks.json"), json.replace("minecraft:village_plains", (String) found[2]), StandardCharsets.UTF_8);
        Tour.command("barelyquesting reload");
        Thread.sleep(800);
        slow("tp @p " + found[0] + " 200 " + found[1]);
        Thread.sleep(6000);
        waitFor("structure task", () -> done(q, team, "t_struct"));
        Tour.shot("structure_task");
        return;
        }
        openBook();
        Tour.onClient(() -> Tour.ui().select("t_check"));
        Thread.sleep(500);
        Tour.shot("checkmark_task");
        Tour.press("Check");
        waitFor("checkmark task", () -> done(q, team, "t_check"));
        Tour.shot("tasks_done");
        Tour.onClient(() -> McGui.setScreen(null));
    }

    /**
     * Finds a structure that really generated near the spawn without a wide search: for each structure the generator's own placement says
     * which chunk may hold it in the four regions around the origin; that chunk is generated and checked for a start. Returns
     * {blockX, blockZ, id} or null.
     */
    @SuppressWarnings("unchecked")
    private static Object[] nearestStructure() throws Exception {
        net.minecraft.server.level.ServerLevel level = player().level() instanceof net.minecraft.server.level.ServerLevel l ? l : null;
        Object registry = callAny(level.registryAccess(), net.minecraft.core.registries.Registries.STRUCTURE, "lookupOrThrow", "registryOrThrow");
        var state = level.getChunkSource().getGeneratorState();
        for (String id : new String[] {"ruined_portal", "village_plains", "village_taiga", "village_savanna", "village_desert", "village_snowy", "pillager_outpost", "swamp_hut", "desert_pyramid", "igloo"}) {
            net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure> holder;
            try {
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.STRUCTURE, Mc.id("minecraft", id));
                holder = (net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>) callAny(registry, key, "getOrThrow", "getHolderOrThrow");
            } catch (ReflectiveOperationException | RuntimeException e) {
                continue;
            }
            for (var placement : state.getPlacementsForStructure(holder)) {
                if (!(placement instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread)) {
                    continue;
                }
                for (int rx = -1; rx <= 0; rx++) {
                    for (int rz = -1; rz <= 0; rz++) {
                        net.minecraft.world.level.ChunkPos pos = spread.getPotentialStructureChunk(state.getLevelSeed(), rx, rz);
                        var chunk = level.getChunkAt(new net.minecraft.core.BlockPos(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ()));
                        var start = chunk.getAllStarts().get(holder.value());
                        Tour.log("candidate " + id + " at " + pos.getMiddleBlockX() + "," + pos.getMiddleBlockZ() + (start != null && start.isValid() ? " has a start" : " has none"));
                        if (start != null && start.isValid()) {
                            return new Object[] {pos.getMiddleBlockX(), pos.getMiddleBlockZ(), "minecraft:" + id};
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Object callAny(Object target, Object arg, String... names) throws ReflectiveOperationException {
        for (String name : names) {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(arg)) {
                    return m.invoke(target, arg);
                }
            }
        }
        throw new NoSuchMethodException(String.join("/", names) + " on " + target.getClass());
    }

    /** Runs a command as the player and returns the text it printed. */
    private static String locate(String command) throws Exception {
        StringBuilder out = new StringBuilder();
        net.minecraft.commands.CommandSource sink = new net.minecraft.commands.CommandSource() {
            @Override
            public void sendSystemMessage(net.minecraft.network.chat.Component message) {
                out.append(message.getString()).append(' ');
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        MinecraftServer server = mc.getSingleplayerServer();
        onServer(() -> {
            server.getCommands().performPrefixedCommand(player().createCommandSourceStack().withSource(sink), command);
            return null;
        }, 170);
        return out.toString();
    }

    private static boolean done(com.itmecasey.barelyquesting.core.net.QuestServer q, String team, String quest) {
        try {
            return onServer(() -> q.engine().isComplete(team, quest));
        } catch (Exception e) {
            return false;
        }
    }

    private static String quest(String id, int x, String task) {
        JsonObject o = new JsonObject();
        return "{\"id\":\"" + id + "\",\"title\":\"" + id + "\",\"icon\":\"minecraft:map\",\"x\":" + x + ",\"y\":0,\"tasks\":[" + task + "]}";
    }
}

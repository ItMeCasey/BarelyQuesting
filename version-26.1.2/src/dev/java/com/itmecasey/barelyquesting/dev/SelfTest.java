package com.itmecasey.barelyquesting.dev;

import com.itmecasey.barelyquesting.compat.Mc;
import com.itmecasey.barelyquesting.compat.McPlayer;
import com.itmecasey.barelyquesting.compat.ServerHooks;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.net.Msg;
import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.task.Matchers;
import com.itmecasey.barelyquesting.core.task.TaskEvent;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Headless self test (Minecraft 26.1.2): real registries, a real ServerPlayer, the engine and the commands. Prints SELFTEST lines. */
public final class SelfTest {
    private static int failures;
    private static int passes;

    private SelfTest() {
    }

    static void check(String name, boolean ok) {
        System.out.println("SELFTEST " + (ok ? "PASS " : "FAIL ") + name);
        if (ok) {
            passes++;
        } else {
            failures++;
        }
    }

    public static void run(MinecraftServer server) {
        try {
            cases(server);
        } catch (Throwable t) {
            t.printStackTrace();
            check("no exception: " + t, false);
        }
        System.out.println("SELFTEST DONE failures=" + failures + " passes=" + passes);
        server.halt(false);
    }

    private static void cases(MinecraftServer server) throws Exception {
        // ---- the mod started on this server ----
        check("quests running", ServerHooks.running());
        QuestServer live = ServerHooks.quests();
        check("starter book loaded", live.engine().book().allQuests().size() >= 9);
        check("quest files written", Files.exists(live.store().dir().resolve("book.json")) && Files.exists(live.store().chapterFile("getting_started")));

        // ---- every item the starter book names exists ----
        boolean items = true;
        for (Quest q : live.engine().book().allQuests()) {
            items &= known(q.icon);
            for (Def d : q.tasks) {
                if (d.params.has("item")) {
                    items &= known(d.item("item"));
                }
            }
            for (Def d : q.rewards) {
                if (d.type.equals("item")) {
                    items &= known(d.item("item"));
                }
            }
        }
        check("starter book items and tags exist", items);

        // ---- tags and data matching ----
        check("item tag matches", Matchers.itemMatches(ItemSpec.parse("#minecraft:logs"), ItemSpec.of("minecraft:oak_log")));
        check("item tag rejects", !Matchers.itemMatches(ItemSpec.parse("#minecraft:logs"), ItemSpec.of("minecraft:stone")));
        check("entity tag matches", Matchers.idMatches("entity", "#minecraft:skeletons", "minecraft:skeleton"));
        check("entity id matches", Matchers.idMatches("entity", "minecraft:zombie", "minecraft:zombie"));
        check("biome tag matches", Matchers.idMatches("biome", "#minecraft:is_forest", "minecraft:forest"));
        check("registry lists", Mc.itemIds().size() > 500 && Mc.entityIds().contains("minecraft:zombie"));

        // ---- a real player ----
        ServerPlayer sp = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.nameUUIDFromBytes("selftest".getBytes()), "selftest"),
                ClientInformation.createDefault());
        McPlayer p = new McPlayer(sp);
        p.giveItem(ItemSpec.of("minecraft:cobblestone", 70));
        check("give and count items", p.countItems(ItemSpec.of("minecraft:cobblestone")) == 70);
        check("take items", p.takeItems(ItemSpec.of("minecraft:cobblestone"), 20) == 20 && p.countItems(ItemSpec.of("minecraft:cobblestone")) == 50);
        p.giveItem(ItemSpec.of("minecraft:oak_log", 3));
        p.giveItem(ItemSpec.of("minecraft:birch_log", 2));
        check("count by tag", p.countItems(ItemSpec.parse("#minecraft:logs")) == 5);
        ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Quest Blade"));
        ItemSpec named1 = Mc.specOf(named, server.registryAccess());
        check("item data is captured", named1.data() != null && named1.data().contains("Quest Blade"));
        ItemSpec strict = new ItemSpec(named1.id(), 1, false, named1.data(), true);
        check("data match accepts the named sword", Mc.matches(strict, named, server.registryAccess()));
        check("data match rejects a plain sword", !Mc.matches(strict, new ItemStack(Items.DIAMOND_SWORD), server.registryAccess()));
        check("stack from spec keeps data", Mc.matches(strict, Mc.stack(strict, server.registryAccess(), 0), server.registryAccess()));
        p.addExperienceLevels(2);
        check("experience", sp.experienceLevel == 2);
        p.runCommand("tag @s add bq_selftest");
        check("command runs with the player as @s", sp.entityTags().contains("bq_selftest"));
        check("dimension and biome", p.dimension().equals("minecraft:overworld") && !p.biome().isEmpty());
        check("advancement and statistic lookups", !p.hasAdvancement("minecraft:story/root") && p.statistic("custom", "minecraft:jump") == 0
                && !p.hasAdvancement("minecraft:nope"));
        check("structure lookup does not throw", !p.inStructure("minecraft:village_plains") || true);

        // ---- the engine with the real player and real registries ----
        Path dir = Path.of("selftest-" + System.nanoTime());
        List<byte[]> sent = new ArrayList<>();
        QuestServer qs = new QuestServer(dir.resolve("config"), dir.resolve("world"), (u, m) -> sent.add(m), new QuestEngine.Host() {
            @Override
            public PlayerView online(UUID id) {
                return id.equals(sp.getUUID()) ? p : null;
            }

            @Override
            public long nowMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public List<PlayerView> players() {
                return List.of(p);
            }
        });
        qs.start();
        qs.onJoin(p);
        check("join sync sends info, book, team and progress", has(sent, Msg.S_INFO) && has(sent, Msg.S_BOOK) && has(sent, Msg.S_TEAM) && has(sent, Msg.S_PROGRESS));
        String team = qs.engine().teams().teamOf(p.uuid()).id;
        qs.engine().event(TaskEvent.pickup(p, Mc.specOf(new ItemStack(Items.OAK_LOG, 4), server.registryAccess())));
        check("obtain with a tag completes the quest", qs.engine().isComplete(team, "wood"));
        int before = p.countItems(ItemSpec.of("minecraft:apple"));
        qs.engine().claim(p, "wood", null);
        check("claim gives the item reward", p.countItems(ItemSpec.of("minecraft:apple")) == before + 8);
        qs.engine().event(TaskEvent.craft(p, Mc.specOf(new ItemStack(Items.CRAFTING_TABLE), server.registryAccess())));
        check("craft event completes the next quest", qs.engine().isComplete(team, "bench"));
        qs.engine().claim(p, "bench", null);
        check("xp reward gives levels", sp.experienceLevel == 7);
        qs.engine().event(TaskEvent.craft(p, Mc.specOf(new ItemStack(Items.WOODEN_PICKAXE), server.registryAccess())));
        qs.engine().poll(p);
        check("have-items task needs no poll for the submit quest", qs.engine().submit(p, "stone", "t0") == 16 && qs.engine().isComplete(team, "stone"));
        qs.engine().event(TaskEvent.kill(p, "minecraft:zombie"));
        check("kill counts", qs.engine().taskProgress(team, "zombie", "t0") == 1);
        qs.flush();
        qs.saveAll();
        check("progress saved", Files.exists(dir.resolve("world/progress.json")) && Files.size(dir.resolve("world/progress.json")) > 10);

        // ---- commands ----
        var dispatcher = server.getCommands().getDispatcher();
        check("commands registered", dispatcher.getRoot().getChild("barelyquesting") != null && dispatcher.getRoot().getChild("bq") != null);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "barelyquesting reload");
        check("reload command keeps the quests", ServerHooks.quests() == live && live.engine().book().allQuests().size() >= 9);
        check("import command reports failure for a missing path", importFails(server));
    }

    private static boolean importFails(MinecraftServer server) {
        var r = com.itmecasey.barelyquesting.core.imports.Importer.importPath(ServerHooks.quests(), Path.of("does-not-exist"));
        return r.error != null;
    }

    private static boolean has(List<byte[]> sent, int id) {
        for (byte[] m : sent) {
            if ((m[0] & 0xff) == id) {
                return true;
            }
        }
        return false;
    }

    private static boolean known(ItemSpec s) {
        if (s.isTag()) {
            boolean[] any = {false};
            Mc.forEachTagMember(s.id(), (id, item) -> any[0] = true);
            return any[0];
        }
        return Mc.item(s.id()) != null;
    }
}

package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.Progress;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.progress.QuestEngine.Status;
import com.itmecasey.barelyquesting.core.script.ScriptRegistry;
import com.itmecasey.barelyquesting.core.task.TaskEvent;
import com.itmecasey.barelyquesting.core.team.TeamManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EngineTest {
    Book book;
    Chapter chapter;
    QuestEngine engine;
    FakePlayer alice = new FakePlayer("alice");
    FakePlayer bob = new FakePlayer("bob");
    long now = 1_000;
    List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        book = new Book();
        chapter = new Chapter("c");
        book.chapters.add(chapter);
        engine = new QuestEngine(book, new TeamManager(), new Progress(), new QuestEngine.Host() {
            public PlayerView online(UUID id) {
                return id.equals(alice.id) ? alice : id.equals(bob.id) ? bob : null;
            }

            public long nowMillis() {
                return now;
            }

            public List<PlayerView> players() {
                return List.of(alice, bob);
            }
        });
        engine.teams().rememberName(alice.id, "alice");
        engine.teams().rememberName(bob.id, "bob");
        engine.addListener(new QuestEngine.Listener() {
            public void questCompleted(String team, Quest q) {
                events.add("done:" + q.id);
            }

            public void questStarted(String team, Quest q) {
                events.add("start:" + q.id);
            }

            public void questClaimed(String team, UUID p, Quest q, Def r) {
                events.add("claim:" + q.id + ":" + r.id);
            }
        });
    }

    Quest quest(String id, Def... tasks) {
        Quest q = new Quest(id);
        for (Def t : tasks) {
            q.tasks.add(t);
        }
        chapter.quests.add(q);
        return q;
    }

    String team(FakePlayer p) {
        return engine.teams().teamOf(p.id).id;
    }

    @Test
    void haveItemsPollsAndCompletes() {
        Quest q = quest("a", Def.of("t", "item").with("item", "3 minecraft:stone"));
        assertEquals(Status.AVAILABLE, engine.status(team(alice), alice.id, q));
        alice.inventory.put("minecraft:stone", 2);
        engine.poll(alice);
        assertEquals(2, engine.taskProgress(team(alice), "a", "t"));
        assertEquals(Status.IN_PROGRESS, engine.status(team(alice), alice.id, q));
        alice.inventory.put("minecraft:stone", 5);
        engine.poll(alice);
        assertEquals(Status.CLAIMED, engine.status(team(alice), alice.id, q)); // no rewards, so nothing left to claim
        assertEquals(List.of("start:a", "done:a"), events);
        assertEquals(1, alice.toasts.size());
    }

    @Test
    void submitConsumesItems() {
        quest("a", Def.of("t", "item").with("item", "4 minecraft:cobblestone").with("consume", true));
        alice.inventory.put("minecraft:cobblestone", 3);
        engine.poll(alice);
        assertEquals(0, engine.taskProgress(team(alice), "a", "t"), "consume tasks are not polled");
        assertEquals(3, engine.submit(alice, "a", "t"));
        assertEquals(0, alice.inventory.get("minecraft:cobblestone"));
        alice.inventory.put("minecraft:cobblestone", 9);
        assertEquals(1, engine.submit(alice, "a", "t"));
        assertEquals(8, alice.inventory.get("minecraft:cobblestone"));
        assertTrue(engine.isComplete(team(alice), "a"));
    }

    @Test
    void craftKillAdvancementStatisticEvents() {
        quest("craft", Def.of("t", "craft").with("item", "2 minecraft:bread"));
        quest("kill", Def.of("t", "kill").with("entity", "minecraft:zombie").with("count", 2));
        quest("adv", Def.of("t", "advancement").with("advancement", "minecraft:story/mine_stone"));
        quest("stat", Def.of("t", "statistic").with("stat_type", "custom").with("stat", "minecraft:jump").with("value", 10));
        engine.event(TaskEvent.craft(alice, ItemSpec.of("minecraft:bread", 2)));
        engine.event(TaskEvent.kill(alice, "minecraft:zombie"));
        engine.event(TaskEvent.kill(alice, "minecraft:skeleton"));
        assertTrue(engine.isComplete(team(alice), "craft"));
        assertFalse(engine.isComplete(team(alice), "kill"));
        engine.event(TaskEvent.kill(alice, "minecraft:zombie"));
        assertTrue(engine.isComplete(team(alice), "kill"));
        alice.advancements.add("minecraft:story/mine_stone");
        alice.stats.put("custom:minecraft:jump", 25L);
        engine.poll(alice);
        assertTrue(engine.isComplete(team(alice), "adv"));
        assertTrue(engine.isComplete(team(alice), "stat"));
    }

    @Test
    void obtainCountsPickupAndCraft() {
        quest("o", Def.of("t", "obtain").with("item", "2 minecraft:apple"));
        engine.event(TaskEvent.pickup(alice, ItemSpec.of("minecraft:apple", 1)));
        engine.event(TaskEvent.craft(alice, ItemSpec.of("minecraft:apple", 1)));
        assertTrue(engine.isComplete(team(alice), "o"));
    }

    @Test
    void locationDimensionBiomeStructure() {
        quest("loc", Def.of("t", "location").with("x", 100).with("y", 64).with("z", 100).with("radius", 5));
        quest("dim", Def.of("t", "dimension").with("dimension", "minecraft:the_nether"));
        quest("bio", Def.of("t", "biome").with("biome", "minecraft:desert"));
        quest("str", Def.of("t", "structure").with("structure", "minecraft:village_plains"));
        engine.poll(alice);
        assertFalse(engine.isComplete(team(alice), "loc"));
        alice.x = 102;
        alice.z = 99;
        alice.dimension = "minecraft:the_nether";
        alice.biome = "minecraft:desert";
        alice.structures.add("minecraft:village_plains");
        engine.poll(alice);
        for (String id : new String[] {"loc", "dim", "bio", "str"}) {
            assertTrue(engine.isComplete(team(alice), id), id);
        }
    }

    @Test
    void dependenciesAndLocks() {
        quest("a", Def.of("t", "checkmark"));
        Quest b = quest("b", Def.of("t", "checkmark"));
        b.dependencies.add("a");
        Quest c = quest("c", Def.of("t", "checkmark"));
        c.locks.add(Def.of("l", "time").with("after", "2026-01-01T00:00:00Z"));
        String t = team(alice);
        assertEquals(Status.LOCKED, engine.status(t, alice.id, b));
        assertFalse(engine.check(alice, "b", "t"), "locked quests cannot be ticked");
        assertTrue(engine.check(alice, "a", "t"));
        assertEquals(Status.AVAILABLE, engine.status(t, alice.id, b));
        assertEquals(Status.LOCKED, engine.status(t, alice.id, c), "time lock: 1970 is before 2026");
        now = java.time.Instant.parse("2026-06-01T00:00:00Z").toEpochMilli();
        assertEquals(Status.AVAILABLE, engine.status(t, alice.id, c));
        Quest d = quest("d", Def.of("t", "checkmark"));
        d.locks.add(Def.of("l", "quest").with("quest", "b"));
        assertEquals(Status.LOCKED, engine.status(t, alice.id, d));
        engine.check(alice, "b", "t");
        assertEquals(Status.AVAILABLE, engine.status(t, alice.id, d));
    }

    @Test
    void scriptLockUsesCallback() {
        Quest a = quest("a", Def.of("t", "checkmark"));
        a.locks.add(Def.of("l", "script").with("name", "closed"));
        assertEquals(Status.AVAILABLE, engine.status(team(alice), alice.id, a), "unknown script lock stays open");
        ScriptRegistry.lock("closed", (l, team, q) -> true);
        assertEquals(Status.LOCKED, engine.status(team(alice), alice.id, a));
        ScriptRegistry.clear();
    }

    @Test
    void anyTaskAndOptionalDependency() {
        Quest a = quest("a", Def.of("x", "checkmark"), Def.of("y", "checkmark"));
        a.anyTask = true;
        Quest opt = quest("opt", Def.of("t", "checkmark"));
        opt.optional = true;
        Quest b = quest("b", Def.of("t", "checkmark"));
        b.dependencies.add("opt");
        assertEquals(Status.AVAILABLE, engine.status(team(alice), alice.id, b), "optional dependencies do not block");
        engine.check(alice, "a", "y");
        assertTrue(engine.isComplete(team(alice), "a"));
    }

    @Test
    void claimGivesRewardsOncePerScope() {
        Quest q = quest("a", Def.of("t", "checkmark"));
        q.rewards.add(Def.of("item", "item").with("item", "2 minecraft:diamond"));
        q.rewards.add(Def.of("xp", "xp").with("amount", 30));
        q.rewards.add(Def.of("lvl", "xp").with("amount", 2).with("unit", "levels"));
        q.rewards.add(Def.of("cmd", "command").with("command", "/say hi {player}"));
        q.rewards.add(Def.of("shared", "item").with("item", "minecraft:gold_ingot").with("scope", "team"));
        assertEquals(0, engine.claim(alice, "a", null), "not complete yet");
        engine.check(alice, "a", "t");
        assertEquals(Status.COMPLETE, engine.status(team(alice), alice.id, q));
        assertEquals(5, engine.claim(alice, "a", null));
        assertEquals(0, engine.claim(alice, "a", null));
        assertEquals(List.of("2x minecraft:diamond", "minecraft:gold_ingot"), alice.given);
        assertEquals(30, alice.xpPoints);
        assertEquals(2, alice.xpLevels);
        assertEquals(List.of("say hi alice"), alice.commands);
        assertEquals(Status.CLAIMED, engine.status(team(alice), alice.id, q));
    }

    @Test
    void teamsShareProgressAndSplitRewards() {
        Quest q = quest("a", Def.of("t", "checkmark"));
        q.rewards.add(Def.of("mine", "item").with("item", "minecraft:apple"));
        q.rewards.add(Def.of("shared", "item").with("item", "minecraft:gold_ingot").with("scope", "team"));
        assertNull(engine.teams().create(alice.id, "Builders"));
        assertNull(engine.teams().invite(alice.id, bob.id));
        assertNull(engine.teams().join(bob.id, team(alice)));
        assertEquals(team(alice), team(bob));
        engine.check(bob, "a", "t");
        assertTrue(engine.isComplete(team(alice), "a"), "bob's work counts for alice");
        engine.claim(alice, "a", null);
        engine.claim(bob, "a", null);
        assertEquals(List.of("minecraft:apple", "minecraft:gold_ingot"), alice.given);
        assertEquals(List.of("minecraft:apple"), bob.given, "team reward only once");
        assertNull(engine.teams().leave(bob.id));
        assertFalse(engine.isComplete(team(bob), "a"), "leaving returns to own progress");
    }

    @Test
    void autoRewardsAndForceComplete() {
        Quest q = quest("a", Def.of("t", "checkmark"));
        q.rewards.add(Def.of("r", "item").with("item", "minecraft:apple").with("auto", true));
        assertTrue(engine.forceComplete(team(alice), "a"));
        assertEquals(List.of("minecraft:apple"), alice.given);
        assertFalse(engine.forceComplete(team(alice), "a"));
        engine.reset(team(alice), "a");
        assertFalse(engine.isComplete(team(alice), "a"));
    }

    @Test
    void progressRoundTripsThroughJson() {
        quest("a", Def.of("t", "item").with("item", "4 minecraft:stone"));
        alice.inventory.put("minecraft:stone", 2);
        engine.poll(alice);
        Progress copy = new Progress();
        copy.fromJson(engine.progress().toJson());
        assertEquals(2, copy.get(team(alice), "a").task("t"));
        assertEquals(engine.progress().toJson(), copy.toJson());
    }

    @Test
    void teamManagerRules() {
        TeamManager tm = engine.teams();
        assertEquals("bad_name", tm.create(alice.id, "  "));
        assertNull(tm.create(alice.id, "Crew"));
        assertEquals("already_in_team", tm.create(alice.id, "Other"));
        assertEquals("not_owner", tm.invite(bob.id, alice.id));
        assertEquals("no_invite", tm.join(bob.id, team(alice)));
        tm.invite(alice.id, bob.id);
        tm.join(bob.id, team(alice));
        assertEquals("not_owner", tm.kick(bob.id, alice.id));
        assertNull(tm.kick(alice.id, bob.id));
        assertTrue(tm.teamOf(bob.id).solo);
        TeamManager copy = new TeamManager();
        copy.fromJson(tm.toJson());
        assertEquals("Crew", copy.teamOf(alice.id).name);
        assertNull(tm.disband(alice.id));
        assertTrue(tm.teamOf(alice.id).solo);
    }
}

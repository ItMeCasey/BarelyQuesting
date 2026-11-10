package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.script.ModJsBridge;
import com.itmecasey.barelyquesting.core.script.QuestApi;
import com.itmecasey.barelyquesting.core.script.ScriptEvents;
import com.itmecasey.barelyquesting.core.script.ScriptRegistry;
import com.itmecasey.barelyquesting.core.task.TaskEvent;
import com.itmecasey.barelyquesting.core.task.TaskTypes;
import com.itmecasey.barelyquesting.core.progress.Rewards;
import com.itmecasey.modjs.script.ExtraBindings;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Runs real JavaScript (Rhino, the engine ModJS and KubeJS use) against the QuestApi through the reflective ModJS bridge. */
class ScriptBridgeTest {
    @TempDir
    Path dir;
    FakePlayer alice = new FakePlayer("alice");
    QuestServer server;

    @BeforeEach
    void setUp() {
        server = new QuestServer(dir.resolve("config"), dir.resolve("world"), (p, m) -> { }, new QuestEngine.Host() {
            public PlayerView online(UUID id) {
                return id.equals(alice.id) ? alice : null;
            }

            public long nowMillis() {
                return 0;
            }

            public List<PlayerView> players() {
                return List.of(alice);
            }
        });
        // Startup scripts run before the server: this change waits for attach().
        QuestApi pre = new QuestApi();
        pre.chapter("from_startup", "From Startup", "minecraft:redstone");
        pre.quest("from_startup", "{\"id\":\"s1\",\"title\":\"Queued\",\"tasks\":[{\"id\":\"t\",\"type\":\"checkmark\"}]}");
        server.start();
        QuestApi.attach(server);
    }

    @AfterEach
    void tearDown() {
        QuestApi.detach();
        ScriptEvents.clear();
        ScriptRegistry.clear();
        TaskTypes.resetToBuiltins();
        Rewards.resetToBuiltins();
        ExtraBindings.PROVIDERS.clear();
    }

    Object run(String js) {
        assertTrue(ModJsBridge.install(new QuestApi()));
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            ExtraBindings.PROVIDERS.get(0).install(com.itmecasey.modjs.script.ScriptType.SERVER, cx, scope);
            return cx.evaluateString(scope, js, "test.js", 1, null);
        } finally {
            Context.exit();
        }
    }

    @Test
    void startupScriptsQueueUntilTheServerRuns() {
        assertNotNull(server.engine().book().quest("s1"));
        assertEquals("From Startup", server.engine().book().chapter("from_startup").title);
    }

    @Test
    void jsRegistersTasksRewardsLocksAndEvents() {
        run("BarelyQuesting.chapter('js', 'JS Chapter', 'minecraft:gold_ingot');"
                + "BarelyQuesting.quest('js', JSON.stringify({id: 'jq', title: 'Fly', "
                + " tasks: [{id: 't', type: 'scripted', name: 'high', count: 5}, {id: 'u', type: 'jumps', count: 3}],"
                + " rewards: [{id: 'r', type: 'scripted', name: 'gift'}, {id: 'v', type: 'thanks'}]}));"
                + "BarelyQuesting.registerTask('high', ctx => ctx.y() > 100 ? ctx.target() : ctx.current());"
                + "BarelyQuesting.newTaskType('jumps', 3, ctx => ctx.stat('custom', 'minecraft:jump') >= 3 ? 3 : 0);"
                + "BarelyQuesting.registerReward('gift', ctx => { ctx.giveItem('2 minecraft:diamond'); ctx.addLevels(1); });"
                + "BarelyQuesting.newRewardType('thanks', ctx => ctx.message('thanks ' + ctx.playerName()));"
                + "BarelyQuesting.registerLock('never', ctx => true);"
                + "BarelyQuesting.onQuestCompleted(e => java.lang.System.setProperty('bq.done', e.questId()));");
        QuestEngine e = server.engine();
        String team = e.teams().teamOf(alice.id).id;
        assertNotNull(e.book().quest("jq"));
        e.poll(alice);
        assertEquals(0, e.taskProgress(team, "jq", "t"));
        alice.y = 150;
        alice.stats.put("custom:minecraft:jump", 4L);
        e.poll(alice);
        assertTrue(e.isComplete(team, "jq"), "both script tasks reached their targets");
        assertEquals("jq", System.getProperty("bq.done"));
        e.claim(alice, "jq", null);
        assertEquals(List.of("2x minecraft:diamond"), alice.given);
        assertEquals(1, alice.xpLevels);
        assertEquals(List.of("thanks alice"), alice.messages);
        run("BarelyQuesting.quest('js', JSON.stringify({id: 'locked', title: 'L', locks: [{id: 'l', type: 'script', name: 'never'}]}));");
        assertFalse(e.isUnlocked(team, e.book().quest("locked")));
    }

    @Test
    void jsEventTasksAndPlayerHelpers() {
        run("BarelyQuesting.chapter('js', 'JS', 'minecraft:book');"
                + "BarelyQuesting.quest('js', JSON.stringify({id: 'ev', title: 'E', tasks: [{id: 't', type: 'scripted', name: 'zombies', count: 2}]}));"
                + "BarelyQuesting.registerTaskEvents('zombies', ctx => ctx.kind() == 'kill' && ctx.key() == 'minecraft:zombie' ? 1 : 0);");
        QuestEngine e = server.engine();
        e.event(TaskEvent.kill(alice, "minecraft:zombie"));
        e.event(TaskEvent.kill(alice, "minecraft:cow"));
        String team = e.teams().teamOf(alice.id).id;
        assertEquals(1, e.taskProgress(team, "ev", "t"));
        Object done = run("BarelyQuesting.isComplete('" + alice.id + "', 'ev')");
        assertEquals(false, done);
        run("BarelyQuesting.complete('" + alice.id + "', 'ev')");
        assertTrue(e.isComplete(team, "ev"));
    }

    @Test
    void bridgeIsQuietWithoutModJs() {
        ExtraBindings.PROVIDERS.clear();
        assertTrue(ModJsBridge.install(new QuestApi()), "test doubles are on the classpath here");
    }

    @Test
    void clearListenersStopsOldListenersAfterAScriptReload() {
        int[] fired = {0};
        com.itmecasey.barelyquesting.core.script.QuestApi api = new com.itmecasey.barelyquesting.core.script.QuestApi();
        api.onQuestCompleted(e -> fired[0]++);
        api.clearListeners();
        api.onQuestCompleted(e -> fired[0] += 10);
        com.itmecasey.barelyquesting.core.script.ScriptEvents.fire("questCompleted", "team", new com.itmecasey.barelyquesting.core.model.Quest("q"), null, null);
        org.junit.jupiter.api.Assertions.assertEquals(10, fired[0]);
        com.itmecasey.barelyquesting.core.script.ScriptEvents.clear();
    }
}

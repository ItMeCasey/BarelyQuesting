package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.net.Msg;
import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.itmecasey.barelyquesting.core.net.Wire;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.script.QuestApi;
import com.itmecasey.barelyquesting.core.script.ScriptEvents;
import com.itmecasey.barelyquesting.core.script.ScriptRegistry;
import com.itmecasey.barelyquesting.core.task.TaskEvent;
import com.itmecasey.barelyquesting.core.task.TaskTypes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerTest {
    @TempDir
    Path dir;
    FakePlayer alice = new FakePlayer("alice");
    FakePlayer bob = new FakePlayer("bob");
    List<byte[]> packets = new ArrayList<>();
    QuestServer server;

    QuestEngine.Host host() {
        return new QuestEngine.Host() {
            public PlayerView online(UUID id) {
                return id.equals(alice.id) ? alice : id.equals(bob.id) ? bob : null;
            }

            public long nowMillis() {
                return 0;
            }

            public List<PlayerView> players() {
                return List.of(alice, bob);
            }
        };
    }

    @BeforeEach
    void setUp() {
        server = new QuestServer(dir.resolve("config"), dir.resolve("world"), (p, m) -> packets.add(m), host());
        server.start();
    }

    int count(int id) {
        int n = 0;
        for (byte[] p : packets) {
            if ((p[0] & 0xff) == id) {
                n++;
            }
        }
        return n;
    }

    String ids() {
        StringBuilder sb = new StringBuilder();
        for (byte[] p : packets) {
            sb.append(p[0]).append(',');
        }
        return sb.toString();
    }

    String read(String p) throws IOException {
        return Files.readString(dir.resolve(p));
    }

    @Test
    void startWritesStarterBookAndJoinSyncs() {
        assertTrue(Files.exists(dir.resolve("config/book.json")));
        server.onJoin(alice);
        assertEquals(1, count(Msg.S_INFO));
        assertEquals(1, count(Msg.S_BOOK));
        assertEquals(1, count(Msg.S_TEAM));
        assertEquals(1, count(Msg.S_PROGRESS));
        Wire.In info = new Wire.In(packets.get(0));
        assertEquals(alice.id.toString(), info.str());
        assertFalse(info.bool(), "survival non-op cannot edit");
    }

    @Test
    void editsNeedPermissionAndBroadcast() throws IOException {
        Quest q = new Quest("x");
        String op = Json.compact(EditOps.putQuest("getting_started", q, null));
        server.handle(alice, new Wire.Out(Msg.C_EDIT).str(op).done());
        assertEquals(1, count(Msg.S_RESULT), "refused with a message");
        assertNull(server.engine().book().quest("x"));
        alice.op = true;
        server.handle(alice, new Wire.Out(Msg.C_EDIT).str(op).done());
        assertNotNull(server.engine().book().quest("x"));
        assertEquals(2, count(Msg.S_OP), "both online players get the op");
        assertTrue(read("config/chapters/getting_started.json").contains("\"x\""));
    }

    @Test
    void checkClaimAndProgressSync() {
        server.onJoin(alice);
        server.flush();
        packets.clear();
        server.handle(alice, new Wire.Out(Msg.C_CHECK).str("check").str("t0").done());
        server.flush();
        assertEquals(1, count(Msg.S_PROGRESS), "progress packets " + ids());
        server.handle(alice, new Wire.Out(Msg.C_CLAIM).str("wood").str("").done());
        assertEquals(1, count(Msg.S_RESULT), "results: nothing to claim");
    }

    @Test
    void badPacketsAreIgnored() {
        server.handle(alice, new byte[] {99});
        server.handle(alice, new byte[] {(byte) Msg.C_EDIT, 0x7f, 0x7f, 0x7f, 0x7f});
        server.handle(alice, new byte[] {});
        alice.op = true;
        server.handle(alice, new Wire.Out(Msg.C_EDIT).str("{not json").done());
    }

    @Test
    void teamActionsAndSync() {
        server.onJoin(alice);
        server.onJoin(bob);
        packets.clear();
        server.handle(alice, new Wire.Out(Msg.C_TEAM).str("create").str("Crew").done());
        server.handle(alice, new Wire.Out(Msg.C_TEAM).str("invite").str("bob").done());
        server.handle(bob, new Wire.Out(Msg.C_TEAM).str("join").str(server.engine().teams().teamOf(alice.id).id).done());
        assertEquals(server.engine().teams().teamOf(alice.id).id, server.engine().teams().teamOf(bob.id).id);
        server.flush();
        assertTrue(count(Msg.S_TEAM) >= 2);
    }

    @Test
    void scriptApiBuildsQuestsAndFiresEvents() {
        QuestApi api = new QuestApi();
        QuestApi.attach(server);
        try {
            List<String> seen = new ArrayList<>();
            ScriptEvents.on("questCompleted", e -> seen.add(e.questId()));
            api.chapter("scripted", "Scripted", "minecraft:redstone");
            String id = api.quest("scripted",
                    "{\"title\":\"From a script\",\"tasks\":[{\"id\":\"t\",\"type\":\"scripted\",\"name\":\"fly\",\"count\":3}]}");
            api.scriptedTask("fly", new ScriptRegistry.TaskCallback() {
                @Override
                public long onEvent(Def t, TaskEvent e) {
                    return 0;
                }
            });
            api.addProgress(alice.id.toString(), id, "t", 3);
            assertTrue(api.isComplete(alice.id.toString(), id));
            assertEquals(List.of(id), seen);
            api.taskType("jumps", 5, new ScriptRegistry.TaskCallback() { });
            assertNotNull(TaskTypes.get("jumps"));
            assertTrue(api.removeQuest(id));
            assertFalse(api.removeQuest(id));
        } finally {
            QuestApi.detach();
            ScriptEvents.clear();
            TaskTypes.resetToBuiltins();
            ScriptRegistry.clear();
        }
    }

    @Test
    void progressSurvivesRestart() {
        server.engine().forceComplete(server.engine().teams().teamOf(alice.id).id, "wood");
        server.saveAll();
        QuestServer second = new QuestServer(dir.resolve("config"), dir.resolve("world"), (p, m) -> { }, host());
        second.start();
        assertTrue(second.engine().isComplete("solo-" + alice.id, "wood"), "the solo team id is stable across restarts");
    }

    @Test
    void wireRoundTrip() {
        byte[] m = new Wire.Out(7).str("héllo ☃").bool(true).i(-5).blob(new byte[] {1, 2, 3}).done();
        Wire.In in = new Wire.In(m);
        assertEquals(7, in.id());
        assertEquals("héllo ☃", in.str());
        assertTrue(in.bool());
        assertEquals(-5, in.i());
        assertEquals(3, in.blob().length);
        String big = "abc" + "x".repeat(10000);
        assertEquals(big, Wire.gunzip(Wire.gzip(big)));
    }
}

package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.itmecasey.barelyquesting.core.imports.Importer;
import com.itmecasey.barelyquesting.core.imports.Snbt;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImportTest {
    @TempDir
    Path dir;

    QuestServer server() {
        QuestServer s = new QuestServer(dir.resolve("config"), dir.resolve("world"), (p, m) -> { }, new QuestEngine.Host() {
            public PlayerView online(UUID id) {
                return null;
            }

            public long nowMillis() {
                return 0;
            }

            public List<PlayerView> players() {
                return List.of();
            }
        });
        s.start();
        return s;
    }

    @Test
    void snbtParsesTypesCommentsAndArrays() {
        JsonElement e = Snbt.parse("{\n a: 1b\n b: 2L, c: 3.5d\n s: \"he said \\\"hi\\\"\"\n l: [1, 2, 3]\n arr: [I; 4, 5]\n"
                + " nested: {x: true y: false}\n bare: hello\n 'quoted key': 7\n}");
        assertEquals(1, e.getAsJsonObject().get("a").getAsInt());
        assertEquals(2, e.getAsJsonObject().get("b").getAsLong());
        assertEquals(3.5, e.getAsJsonObject().get("c").getAsDouble());
        assertEquals("he said \"hi\"", e.getAsJsonObject().get("s").getAsString());
        assertEquals(3, e.getAsJsonObject().getAsJsonArray("l").size());
        assertEquals(2, e.getAsJsonObject().getAsJsonArray("arr").size());
        assertTrue(e.getAsJsonObject().getAsJsonObject("nested").get("x").getAsBoolean());
        assertEquals("hello", e.getAsJsonObject().get("bare").getAsString());
        assertEquals(7, e.getAsJsonObject().get("quoted key").getAsInt());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Snbt.parse("{a: [1, 2}"));
    }

    @Test
    void importsFtbQuestsFolder() throws IOException {
        Path root = dir.resolve("ftb/quests");
        Files.createDirectories(root.resolve("chapters"));
        Files.writeString(root.resolve("chapters/start.snbt"), "{\n id: \"AAAA0001\"\n group: \"\"\n order_index: 0\n filename: \"start\"\n"
                + " title: \"Start Here\"\n icon: \"minecraft:grass_block\"\n quests: [\n {\n  id: \"Q1\"\n  title: \"First\"\n  x: 0.0d\n  y: 0.0d\n"
                + "  description: [\"Line one\", \"Line &cTwo\"]\n  tasks: [{id: \"T1\", type: \"item\", item: {id: \"minecraft:stone\", Count: 1b}, count: 8L, consume_items: true}]\n"
                + "  rewards: [{id: \"R1\", type: \"xp\", xp: 100}, {id: \"R2\", type: \"item\", item: \"minecraft:apple\", count: 3}]\n }\n {\n  id: \"Q2\"\n"
                + "  x: 2.0d\n  y: 1.0d\n  dependencies: [\"Q1\"]\n  tasks: [{id: \"T2\", type: \"kill\", entity: \"minecraft:zombie\", value: 5L}, {id: \"T3\", type: \"observation\"}]\n"
                + "  rewards: [{id: \"R3\", type: \"command\", command: \"/give @p diamond\"}, {id: \"R4\", type: \"loot\"}]\n }\n ]\n}\n");
        Files.writeString(root.resolve("chapters/second.snbt"), "{ id: \"AAAA0002\" order_index: 1 filename: \"second\" title: \"Second\" quests: [\n"
                + "{ id: \"Q3\" x: 0.0d y: 0.0d dependencies: [\"Q2\"] tasks: [{id: \"T4\", type: \"checkmark\"}] } ] }");
        QuestServer s = server();
        int before = s.engine().book().allQuests().size();
        Importer.Result r = Importer.importPath(s, root);
        assertNull(r.error, r.error);
        assertEquals("FTB Quests", r.format);
        assertEquals(2, r.chapters);
        assertEquals(3, r.quests);
        assertEquals(before + 3, s.engine().book().allQuests().size());
        Chapter c = s.engine().book().chapter("start_here");
        assertNotNull(c);
        Quest first = c.quests.get(0);
        assertEquals("First", first.title);
        assertEquals("Line one\nLine &cTwo", first.description);
        assertEquals("item", first.tasks.get(0).type);
        assertEquals(8, first.tasks.get(0).item("item").count());
        assertTrue(first.tasks.get(0).bool("consume", false));
        assertEquals("xp", first.rewards.get(0).type);
        assertEquals("minecraft:apple", first.rewards.get(1).item("item").id());
        Quest second = c.quests.get(1);
        assertEquals(List.of(first.id), second.dependencies, "dependencies map to the new ids");
        assertEquals("kill", second.tasks.get(0).type);
        assertEquals("checkmark", second.tasks.get(1).type, "unknown task types become checkmarks");
        assertEquals(1, second.rewards.size(), "unsupported rewards are skipped");
        assertFalse(r.skipped.isEmpty());
        Quest crossChapter = s.engine().book().chapter("second").quests.get(0);
        assertEquals(List.of(second.id), crossChapter.dependencies, "dependencies across chapters work");
        assertTrue(Files.exists(dir.resolve("config/chapters/start_here.json")), "saved to disk");
        // a second import does not clash with ids or chapter names
        Importer.Result again = Importer.importPath(s, root);
        assertNull(again.error);
        assertNotNull(s.engine().book().chapter("start_here_2"));
    }

    @Test
    void importsBetterQuestingDatabase() throws IOException {
        Path root = dir.resolve("bq/DefaultQuests");
        Files.createDirectories(root);
        Files.writeString(root.resolve("QuestDatabase.json"), "{\"format:8\":\"3.0.0\",\"questDatabase:9\":{"
                + "\"0:10\":{\"questID:3\":0,\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Mine Stone\",\"desc:8\":\"Mine some stone.\","
                + "\"icon:10\":{\"id:8\":\"minecraft:stone\",\"Count:3\":1,\"Damage:2\":0}}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\",\"consume:1\":1,\"requiredItems:9\":{\"0:10\":{\"id:8\":\"minecraft:cobblestone\",\"Count:3\":16}}}},"
                + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\",\"rewards:9\":{\"0:10\":{\"id:8\":\"minecraft:iron_ingot\",\"Count:3\":2}}},"
                + "\"1:10\":{\"index:3\":1,\"rewardID:8\":\"bq_standard:xp\",\"amount:3\":5,\"levels:1\":1}}},"
                + "\"1:10\":{\"questID:3\":1,\"preRequisites:11\":[0],\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Kill Zombies\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:hunt\",\"target:8\":\"minecraft:zombie\",\"required:3\":4}}},"
                + "\"2:10\":{\"questID:3\":2,\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Loose\"}},\"tasks:9\":{\"0:10\":{\"taskID:8\":\"bq_standard:checkbox\"}}}}}");
        Files.writeString(root.resolve("QuestLineDatabase.json"), "{\"questLineDatabase:9\":{\"0:10\":{\"lineID:3\":0,\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Basics\"}},"
                + "\"quests:9\":{\"0:10\":{\"id:3\":0,\"x:3\":0,\"y:3\":0},\"1:10\":{\"id:3\":1,\"x:3\":48,\"y:3\":24}}}}}");
        QuestServer s = server();
        Importer.Result r = Importer.importPath(s, dir.resolve("bq"));
        assertNull(r.error, r.error);
        assertEquals("Better Questing", r.format);
        assertEquals(2, r.chapters, "one line plus the Imported chapter for the loose quest");
        Chapter basics = s.engine().book().chapter("basics");
        assertNotNull(basics);
        Quest stone = basics.quests.get(0);
        assertEquals("Mine Stone", stone.title);
        assertEquals("minecraft:stone", stone.icon.id());
        assertEquals(16, stone.tasks.get(0).item("item").count());
        assertEquals(2, stone.rewards.size() - 0);
        assertEquals("levels", stone.rewards.get(1).str("unit", ""));
        Quest zombies = basics.quests.get(1);
        assertEquals(2, zombies.x);
        assertEquals(1, zombies.y);
        assertEquals(List.of(stone.id), zombies.dependencies);
        assertEquals(4, zombies.tasks.get(0).num("count", 0));
        assertNotNull(s.engine().book().chapter("imported"));
    }

    @Test
    void unknownFolderIsAnError() throws IOException {
        Files.createDirectories(dir.resolve("empty"));
        assertNotNull(Importer.importPath(server(), dir.resolve("empty")).error);
        assertNotNull(Importer.importPath(server(), dir.resolve("missing")).error);
    }
}

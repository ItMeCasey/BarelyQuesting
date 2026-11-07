package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.DefaultBook;
import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.store.QuestStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelTest {
    @Test
    void itemSpecParsing() {
        assertEquals(ItemSpec.of("minecraft:stone", 3), ItemSpec.parse("3 minecraft:stone"));
        assertEquals(ItemSpec.of("minecraft:stone", 3), ItemSpec.parse("3x stone"));
        ItemSpec tag = ItemSpec.parse("4 #minecraft:logs");
        assertTrue(tag.isTag());
        assertEquals("minecraft:logs", tag.id());
        assertEquals(tag, ItemSpec.fromJson(tag.toJson()));
        ItemSpec data = new ItemSpec("minecraft:diamond_sword", 1, false, "{Damage:0}", true);
        assertEquals(data, ItemSpec.fromJson(data.toJson()));
        assertEquals("\"minecraft:apple\"", ItemSpec.of("minecraft:apple").toJson().toString());
    }

    @Test
    void bookRoundTripsAndStoreWritesReadableFiles(@TempDir Path dir) throws IOException {
        Book book = DefaultBook.create();
        book.editors.add("casey");
        QuestStore store = new QuestStore(dir);
        assertFalse(store.exists());
        store.save(book);
        assertTrue(Files.exists(dir.resolve("book.json")));
        assertTrue(Files.exists(dir.resolve("chapters/getting_started.json")));
        String text = Files.readString(dir.resolve("chapters/getting_started.json"));
        assertTrue(text.contains("\n  \"title\""), "pretty printed");
        Book back = store.load();
        assertEquals(book.toJson(), back.toJson());
        assertTrue(back.editors.contains("casey"));
        book.chapters.remove(1);
        store.save(book);
        assertFalse(Files.exists(dir.resolve("chapters/exploring.json")), "removed chapters disappear from disk");
        Files.writeString(dir.resolve("chapters/extra.json"), "{\"id\":\"extra\",\"title\":\"Extra\",\"quests\":[]}");
        assertEquals("extra", store.load().chapters.get(1).id, "hand-added chapter files are appended");
    }

    @Test
    void repairDropsMissingAndCyclicDependencies() {
        Book b = new Book();
        Chapter c = new Chapter("c");
        b.chapters.add(c);
        Quest a = new Quest("a");
        Quest q = new Quest("b");
        c.quests.add(a);
        c.quests.add(q);
        a.dependencies.add("b");
        q.dependencies.add("a");
        q.dependencies.add("ghost");
        assertEquals(2, b.repair());
        assertEquals(1, q.dependencies.size() + a.dependencies.size());
    }

    @Test
    void editOpsApplyAndUndo() {
        Book b = DefaultBook.create();
        String before = b.toJson().toString();
        List<JsonObject> undo = new ArrayList<>();

        Quest fresh = new Quest("fresh");
        fresh.dependencies.add("wood");
        EditOps.Result r = EditOps.apply(b, EditOps.putQuest("getting_started", fresh, null));
        assertTrue(r.ok);
        undo.add(r.inverse);
        assertNotNull(b.quest("fresh"));

        r = EditOps.apply(b, EditOps.removeQuest("bench"));
        assertTrue(r.ok);
        undo.add(r.inverse);
        assertNull(b.quest("bench"));
        assertFalse(b.quest("pickaxe").dependencies.contains("bench"));

        Chapter nc = new Chapter("new_one");
        nc.title = "New One";
        r = EditOps.apply(b, EditOps.putChapter(nc, 0));
        assertTrue(r.ok);
        undo.add(r.inverse);
        assertEquals("new_one", b.chapters.get(0).id);

        r = EditOps.apply(b, EditOps.removeChapter("exploring"));
        assertTrue(r.ok);
        undo.add(r.inverse);
        assertNull(b.chapter("exploring"));

        for (int i = undo.size() - 1; i >= 0; i--) {
            assertTrue(EditOps.apply(b, undo.get(i)).ok);
        }
        assertEquals(before, b.toJson().toString());
    }

    @Test
    void putQuestRejectsCyclesAndMovesBetweenChapters() {
        Book b = DefaultBook.create();
        Quest wood = b.quest("wood").copy();
        wood.dependencies.add("pickaxe");
        assertTrue(EditOps.apply(b, EditOps.putQuest("getting_started", wood, null)).ok);
        assertTrue(b.quest("wood").dependencies.isEmpty(), "a dependency that would make a cycle is dropped");
        Quest moved = b.quest("nether").copy();
        EditOps.Result r = EditOps.apply(b, EditOps.putQuest("getting_started", moved, null));
        assertTrue(r.ok);
        assertEquals("getting_started", b.chapterOf("nether").id);
        assertTrue(EditOps.apply(b, r.inverse).ok);
        assertEquals("exploring", b.chapterOf("nether").id);
        assertFalse(EditOps.apply(b, EditOps.removeQuest("nope")).ok);
    }

    @Test
    void batchRollsBackOnFailure() {
        Book b = DefaultBook.create();
        String before = b.toJson().toString();
        List<JsonObject> ops = new ArrayList<>();
        ops.add(EditOps.removeQuest("wood"));
        ops.add(EditOps.removeQuest("missing"));
        assertFalse(EditOps.apply(b, EditOps.batch(ops)).ok);
        assertEquals(before, b.toJson().toString());
    }
}

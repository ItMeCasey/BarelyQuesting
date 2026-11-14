package com.itmecasey.barelyquesting.core.edit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Quest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Every change to the book is one of these JSON operations. The server applies an operation, saves the files and sends the same
 * operation to every client, so all copies stay equal. Each apply returns the inverse operation, which is what undo sends.
 *
 * <pre>
 * {"op":"putQuest","chapter":"c","quest":{...},"index":0}      add or replace a quest (moves it if it lives in another chapter)
 * {"op":"removeQuest","quest":"id"}                            remove a quest and every dependency on it
 * {"op":"putChapter","chapter":{id,title,icon,group},"index":0} add or update a chapter header
 * {"op":"removeChapter","chapter":"id"}                        remove a chapter with its quests
 * {"op":"moveChapter","chapter":"id","index":2}
 * {"op":"setBook","title":"...","creative_edits":true,"editors":["name"]}
 * {"op":"batch","ops":[...]}
 * </pre>
 */
public final class EditOps {
    /** Outcome of applying one operation. */
    public static final class Result {
        public final boolean ok;
        public final String error;
        public final JsonObject inverse;

        Result(boolean ok, String error, JsonObject inverse) {
            this.ok = ok;
            this.error = error;
            this.inverse = inverse;
        }

        static Result fail(String error) {
            return new Result(false, error, null);
        }

        static Result ok(JsonObject inverse) {
            return new Result(true, null, inverse);
        }
    }

    private EditOps() {
    }

    public static JsonObject putQuest(String chapter, Quest quest, Integer index) {
        JsonObject o = new JsonObject();
        o.addProperty("op", "putQuest");
        o.addProperty("chapter", chapter);
        o.add("quest", quest.toJson());
        if (index != null) {
            o.addProperty("index", index);
        }
        return o;
    }

    public static JsonObject removeQuest(String quest) {
        JsonObject o = new JsonObject();
        o.addProperty("op", "removeQuest");
        o.addProperty("quest", quest);
        return o;
    }

    public static JsonObject putChapter(Chapter chapter, Integer index) {
        JsonObject o = new JsonObject();
        o.addProperty("op", "putChapter");
        o.add("chapter", chapter.headerJson());
        if (index != null) {
            o.addProperty("index", index);
        }
        return o;
    }

    public static JsonObject removeChapter(String chapter) {
        JsonObject o = new JsonObject();
        o.addProperty("op", "removeChapter");
        o.addProperty("chapter", chapter);
        return o;
    }

    public static JsonObject moveChapter(String chapter, int index) {
        JsonObject o = new JsonObject();
        o.addProperty("op", "moveChapter");
        o.addProperty("chapter", chapter);
        o.addProperty("index", index);
        return o;
    }

    public static JsonObject batch(List<JsonObject> ops) {
        JsonObject o = new JsonObject();
        o.addProperty("op", "batch");
        JsonArray a = new JsonArray();
        ops.forEach(a::add);
        o.add("ops", a);
        return o;
    }

    public static Result apply(Book book, JsonObject op) {
        String kind = Json.str(op, "op", "");
        switch (kind) {
            case "putQuest":
                return putQuest(book, op);
            case "removeQuest":
                return removeQuest(book, Json.str(op, "quest", ""));
            case "putChapter":
                return putChapter(book, op);
            case "removeChapter":
                return removeChapter(book, Json.str(op, "chapter", ""));
            case "moveChapter":
                return moveChapter(book, op);
            case "setBook":
                return setBook(book, op);
            case "batch":
                return batch(book, op);
            default:
                return Result.fail("unknown_op");
        }
    }

    private static Result putQuest(Book book, JsonObject op) {
        Chapter chapter = book.chapter(Json.str(op, "chapter", ""));
        JsonObject qo = Json.obj(op, "quest");
        if (chapter == null || qo == null) {
            return Result.fail("no_chapter");
        }
        Quest quest = Quest.fromJson(qo);
        if (quest.id.isEmpty()) {
            return Result.fail("no_id");
        }
        Chapter oldChapter = book.chapterOf(quest.id);
        Quest old = oldChapter == null ? null : oldChapter.quest(quest.id);
        int oldIndex = old == null ? -1 : oldChapter.quests.indexOf(old);
        JsonObject oldJson = old == null ? null : old.toJson();
        quest.dependencies.removeIf(d -> book.quest(d) == null || d.equals(quest.id));
        quest.dependencies.removeIf(d -> book.reaches(d, quest.id, new HashSet<>()));
        List<JsonObject> inverse = new ArrayList<>();
        if (old == null) {
            inverse.add(removeQuest(quest.id));
        } else {
            JsonObject back = new JsonObject();
            back.addProperty("op", "putQuest");
            back.addProperty("chapter", oldChapter.id);
            back.add("quest", oldJson);
            back.addProperty("index", oldIndex);
            inverse.add(back);
        }
        if (old != null) {
            oldChapter.quests.remove(old);
        }
        int idx = op.has("index") ? Json.integer(op, "index", chapter.quests.size()) : (old != null && oldChapter == chapter ? oldIndex : chapter.quests.size());
        idx = Math.max(0, Math.min(idx, chapter.quests.size()));
        chapter.quests.add(idx, quest);
        return Result.ok(inverse.size() == 1 ? inverse.get(0) : batch(inverse));
    }

    private static Result removeQuest(Book book, String id) {
        Chapter chapter = book.chapterOf(id);
        if (chapter == null) {
            return Result.fail("no_quest");
        }
        Quest quest = chapter.quest(id);
        List<JsonObject> undo = new ArrayList<>();
        undo.add(putQuest(chapter.id, quest, chapter.quests.indexOf(quest)));
        // Snapshot the dependents before anything moves, so their saved positions match the original list.
        List<Quest> dependents = new ArrayList<>();
        for (Quest other : book.allQuests()) {
            if (other.dependencies.contains(id)) {
                Chapter oc = book.chapterOf(other.id);
                undo.add(putQuest(oc.id, other, oc.quests.indexOf(other)));
                dependents.add(other);
            }
        }
        chapter.quests.remove(quest);
        for (Quest other : dependents) {
            other.dependencies.remove(id);
        }
        // Undo order: bring the quest back first, then restore the quests that lost the dependency.
        return Result.ok(batchRaw(undo));
    }

    /** A batch that is applied in the listed order (used for inverses that are already in undo order). */
    private static JsonObject batchRaw(List<JsonObject> ops) {
        return ops.size() == 1 ? ops.get(0) : batch(ops);
    }

    private static Result putChapter(Book book, JsonObject op) {
        JsonObject co = Json.obj(op, "chapter");
        if (co == null || Json.str(co, "id", "").isEmpty()) {
            return Result.fail("no_id");
        }
        String id = Json.str(co, "id", "");
        Chapter c = book.chapter(id);
        JsonObject inverse;
        if (c == null) {
            c = new Chapter(id);
            inverse = removeChapter(id);
            int idx = op.has("index") ? Json.integer(op, "index", book.chapters.size()) : book.chapters.size();
            book.chapters.add(Math.max(0, Math.min(idx, book.chapters.size())), c);
        } else {
            inverse = putChapter(c, book.chapters.indexOf(c));
            if (op.has("index")) {
                book.chapters.remove(c);
                int idx = Json.integer(op, "index", book.chapters.size());
                book.chapters.add(Math.max(0, Math.min(idx, book.chapters.size())), c);
            }
        }
        Chapter parsed = Chapter.fromJson(co);
        c.title = parsed.title;
        c.icon = parsed.icon;
        c.group = parsed.group;
        return Result.ok(inverse);
    }

    private static Result removeChapter(Book book, String id) {
        Chapter c = book.chapter(id);
        if (c == null) {
            return Result.fail("no_chapter");
        }
        int index = book.chapters.indexOf(c);
        List<JsonObject> undo = new ArrayList<>();
        undo.add(putChapter(c, index));
        List<JsonObject> inverses = new ArrayList<>();
        for (Quest q : new ArrayList<>(c.quests)) {
            Result r = removeQuest(book, q.id);
            if (r.ok) {
                inverses.add(r.inverse);
            }
        }
        // Undo in reverse order of removal, so dependents see their dependencies again.
        java.util.Collections.reverse(inverses);
        undo.addAll(inverses);
        book.chapters.remove(c);
        return Result.ok(batchRaw(undo));
    }

    private static Result moveChapter(Book book, JsonObject op) {
        Chapter c = book.chapter(Json.str(op, "chapter", ""));
        if (c == null) {
            return Result.fail("no_chapter");
        }
        int old = book.chapters.indexOf(c);
        book.chapters.remove(c);
        int idx = Math.max(0, Math.min(Json.integer(op, "index", old), book.chapters.size()));
        book.chapters.add(idx, c);
        return Result.ok(moveChapter(c.id, old));
    }

    private static Result setBook(Book book, JsonObject op) {
        JsonObject inverse = new JsonObject();
        inverse.addProperty("op", "setBook");
        inverse.addProperty("title", book.title);
        if (op.has("title")) {
            book.title = Json.str(op, "title", book.title);
        }
        return Result.ok(inverse);
    }

    private static Result batch(Book book, JsonObject op) {
        List<JsonObject> inverses = new ArrayList<>();
        for (JsonElement e : Json.arr(op, "ops")) {
            Result r = apply(book, e.getAsJsonObject());
            if (!r.ok) {
                // Roll back what was applied.
                for (int i = inverses.size() - 1; i >= 0; i--) {
                    apply(book, inverses.get(i));
                }
                return r;
            }
            inverses.add(r.inverse);
        }
        java.util.Collections.reverse(inverses);
        return Result.ok(batch(inverses));
    }
}

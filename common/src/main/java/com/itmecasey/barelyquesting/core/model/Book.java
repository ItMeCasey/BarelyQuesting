package com.itmecasey.barelyquesting.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The whole quest book: chapters in order plus a few settings. This is the data the server owns and clients mirror. */
public final class Book {
    public static final int FORMAT = 1;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";

    public String title = "Quest Book";
    public final List<Chapter> chapters = new ArrayList<>();

    public Chapter chapter(String id) {
        for (Chapter c : chapters) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public Quest quest(String questId) {
        for (Chapter c : chapters) {
            Quest q = c.quest(questId);
            if (q != null) {
                return q;
            }
        }
        return null;
    }

    public Chapter chapterOf(String questId) {
        for (Chapter c : chapters) {
            if (c.quest(questId) != null) {
                return c;
            }
        }
        return null;
    }

    public List<Quest> allQuests() {
        List<Quest> out = new ArrayList<>();
        for (Chapter c : chapters) {
            out.addAll(c.quests);
        }
        return out;
    }

    /** A short random id, unique in this book. */
    public String newQuestId() {
        while (true) {
            String id = randomId(8);
            if (quest(id) == null) {
                return id;
            }
        }
    }

    public String newChapterId(String hint) {
        String base = slug(hint);
        if (base.isEmpty()) {
            base = "chapter";
        }
        String id = base;
        int n = 2;
        while (chapter(id) != null) {
            id = base + "_" + n++;
        }
        return id;
    }

    public static String randomId(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String slug(String text) {
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') {
                sb.append('_');
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '_') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /** Header (book.json): title, settings and the chapter order. */
    public JsonObject headerJson() {
        JsonObject o = new JsonObject();
        o.addProperty("format", FORMAT);
        o.addProperty("title", title);
        JsonArray order = new JsonArray();
        for (Chapter c : chapters) {
            order.add(c.id);
        }
        o.add("chapters", order);
        return o;
    }

    /** Everything in one object, for syncing to clients. */
    public JsonObject toJson() {
        JsonObject o = headerJson();
        JsonArray chapterObjects = new JsonArray();
        for (Chapter c : chapters) {
            chapterObjects.add(c.toJson());
        }
        o.add("chapters", chapterObjects);
        return o;
    }

    public static Book fromJson(JsonObject o) {
        Book b = new Book();
        b.title = Json.str(o, "title", b.title);
        for (JsonElement e : Json.arr(o, "chapters")) {
            if (e.isJsonObject()) {
                b.chapters.add(Chapter.fromJson(e.getAsJsonObject()));
            }
        }
        return b;
    }

    public Book copy() {
        return fromJson(toJson());
    }

    /** Removes dependencies that point at missing quests or at themselves and breaks cycles; returns how many were removed. */
    public int repair() {
        int removed = 0;
        Set<String> ids = new HashSet<>();
        for (Quest q : allQuests()) {
            ids.add(q.id);
        }
        for (Quest q : allQuests()) {
            int before = q.dependencies.size();
            q.dependencies.removeIf(d -> !ids.contains(d) || d.equals(q.id));
            removed += before - q.dependencies.size();
        }
        for (Quest q : allQuests()) {
            for (String dep : new ArrayList<>(q.dependencies)) {
                if (reaches(dep, q.id, new HashSet<>())) {
                    q.dependencies.remove(dep);
                    removed++;
                }
            }
        }
        return removed;
    }

    /** True if {@code from} depends (directly or not) on {@code target}. */
    public boolean reaches(String from, String target, Set<String> seen) {
        if (from.equals(target)) {
            return true;
        }
        if (!seen.add(from)) {
            return false;
        }
        Quest q = quest(from);
        if (q == null) {
            return false;
        }
        for (String d : q.dependencies) {
            if (reaches(d, target, seen)) {
                return true;
            }
        }
        return false;
    }
}

package com.itmecasey.barelyquesting.core.imports;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a Better Questing database ({@code QuestDatabase.json} and {@code QuestLineDatabase.json}, usually inside
 * {@code DefaultQuests}) into chapters. BQ stores NBT as JSON: every key carries a type suffix such as {@code name:8}, lists are
 * objects keyed {@code 0:10}, {@code 1:10}. Quest lines become chapters; a quest in no line goes to an "Imported" chapter.
 */
public final class BqImporter {
    public final List<Chapter> chapters = new ArrayList<>();
    public final List<String> skipped = new ArrayList<>();
    private final Book target;

    public BqImporter(Book target) {
        this.target = target;
    }

    public static boolean looks(Path path) {
        return findDatabase(path) != null || splitRoot(path) != null;
    }

    /** The folder of a split export (Quests/ and QuestLines/ with one JSON file per quest), as newer Better Questing versions write it. */
    private static Path splitRoot(Path path) {
        if (!Files.isDirectory(path)) {
            return null;
        }
        for (Path p : new Path[] {path, path.resolve("DefaultQuests")}) {
            if (Files.isDirectory(p.resolve("Quests"))) {
                return p;
            }
        }
        return null;
    }

    private static List<Path> jsonFiles(Path dir) throws IOException {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (java.util.stream.Stream<Path> s = Files.walk(dir)) {
            s.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".json")).sorted().forEach(out::add);
        }
        return out;
    }

    private static String key(JsonObject o, String high, String low) {
        return Json.lng(o, high, 0) + ":" + Json.lng(o, low, 0);
    }

    /**
     * Reads a split export into the shape of the single-file export so one code path handles both: quests get small numbers,
     * prerequisites and quest line entries are rewritten from (high, low) id pairs to those numbers.
     */
    private static JsonObject[] readSplit(Path root) throws IOException {
        JsonObject quests = new JsonObject();
        Map<String, Integer> numbers = new HashMap<>();
        List<JsonObject> list = new ArrayList<>();
        for (Path f : jsonFiles(root.resolve("Quests"))) {
            JsonElement e = clean(Json.parse(Files.readString(f, StandardCharsets.UTF_8)));
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject q = e.getAsJsonObject();
            numbers.put(key(q, "questIDHigh", "questIDLow"), list.size());
            q.addProperty("questID", list.size());
            list.add(q);
        }
        for (JsonObject q : list) {
            JsonArray pre = new JsonArray();
            for (JsonObject ref : values(q.get("preRequisites"))) {
                Integer n = numbers.get(key(ref, "questIDHigh", "questIDLow"));
                if (n != null) {
                    pre.add(n);
                }
            }
            if (q.has("preRequisites") && !q.get("preRequisites").isJsonArray() || !q.has("preRequisites")) {
                q.add("preRequisites", pre);
            }
            quests.add(String.valueOf(q.get("questID").getAsInt()), q);
        }
        JsonObject lines = new JsonObject();
        int index = 0;
        for (Path f : jsonFiles(root.resolve("QuestLines"))) {
            JsonElement e = clean(Json.parse(Files.readString(f, StandardCharsets.UTF_8)));
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject line = e.getAsJsonObject();
            JsonObject entries = new JsonObject();
            int n = 0;
            for (JsonObject entry : values(line.get("quests"))) {
                Integer id = numbers.get(key(entry, "questIDHigh", "questIDLow"));
                if (id == null) {
                    id = numbers.get(key(entry, "id", "id"));
                }
                if (id == null) {
                    continue;
                }
                JsonObject placed = new JsonObject();
                placed.addProperty("id", id);
                placed.addProperty("x", Json.integer(entry, "x", 0));
                placed.addProperty("y", Json.integer(entry, "y", 0));
                entries.add(String.valueOf(n++), placed);
            }
            line.add("quests", entries);
            lines.add(String.valueOf(index++), line);
        }
        JsonObject db = new JsonObject();
        db.add("questDatabase", quests);
        JsonObject lineDb = new JsonObject();
        lineDb.add("questLineDatabase", lines);
        return new JsonObject[] {db, lineDb};
    }

    private static Path findDatabase(Path path) {
        if (Files.isRegularFile(path)) {
            return path.getFileName().toString().equals("QuestDatabase.json") ? path : null;
        }
        for (Path p : new Path[] {path.resolve("QuestDatabase.json"), path.resolve("DefaultQuests").resolve("QuestDatabase.json")}) {
            if (Files.exists(p)) {
                return p;
            }
        }
        return null;
    }

    /** Removes the {@code :<type>} suffix from every key, recursively, so the rest of the code can use plain names. */
    static JsonElement clean(JsonElement e) {
        if (e.isJsonObject()) {
            JsonObject out = new JsonObject();
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
                String k = en.getKey();
                int colon = k.lastIndexOf(':');
                if (colon > 0 && k.substring(colon + 1).matches("\\d+")) {
                    k = k.substring(0, colon);
                }
                out.add(k, clean(en.getValue()));
            }
            return out;
        }
        if (e.isJsonArray()) {
            JsonArray out = new JsonArray();
            e.getAsJsonArray().forEach(x -> out.add(clean(x)));
            return out;
        }
        return e;
    }

    /** Values of an object keyed 0, 1, 2 ... or an array, in order. */
    static List<JsonObject> values(JsonElement e) {
        List<JsonObject> out = new ArrayList<>();
        if (e == null) {
            return out;
        }
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
                if (x.isJsonObject()) {
                    out.add(x.getAsJsonObject());
                }
            }
        } else if (e.isJsonObject()) {
            List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(e.getAsJsonObject().entrySet());
            entries.sort((a, b) -> {
                try {
                    return Integer.compare(Integer.parseInt(a.getKey()), Integer.parseInt(b.getKey()));
                } catch (NumberFormatException ex) {
                    return a.getKey().compareTo(b.getKey());
                }
            });
            for (Map.Entry<String, JsonElement> en : entries) {
                if (en.getValue().isJsonObject()) {
                    out.add(en.getValue().getAsJsonObject());
                }
            }
        }
        return out;
    }

    public void load(Path path) throws IOException {
        Path dbFile = findDatabase(path);
        JsonObject db;
        JsonObject lines = null;
        if (dbFile != null) {
            db = clean(Json.parse(Files.readString(dbFile, StandardCharsets.UTF_8))).getAsJsonObject();
            Path linesFile = dbFile.resolveSibling("QuestLineDatabase.json");
            if (Files.exists(linesFile)) {
                lines = clean(Json.parse(Files.readString(linesFile, StandardCharsets.UTF_8))).getAsJsonObject();
            }
        } else if (splitRoot(path) != null) {
            JsonObject[] split = readSplit(splitRoot(path));
            db = split[0];
            lines = split[1];
        } else {
            throw new IOException("no QuestDatabase.json found");
        }

        Map<Integer, JsonObject> byId = new HashMap<>();
        for (JsonObject q : values(db.get("questDatabase"))) {
            byId.put(Json.integer(q, "questID", -1), q);
        }
        Map<Integer, String> newIds = new HashMap<>();
        for (Integer id : byId.keySet()) {
            newIds.put(id, freshId(newIds));
        }
        Map<Integer, Boolean> placed = new HashMap<>();
        if (lines != null) {
            for (JsonObject line : values(lines.get("questLineDatabase"))) {
                JsonObject props = props(line);
                Chapter c = new Chapter(uniqueChapterId(Json.str(props, "name", "Quest Line")));
                c.title = Json.str(props, "name", "Quest Line");
                c.icon = icon(props.get("icon"));
                for (JsonObject entry : values(line.get("quests"))) {
                    int qid = Json.integer(entry, "id", -1);
                    JsonObject src = byId.get(qid);
                    if (src == null) {
                        continue;
                    }
                    Quest q = quest(src, newIds);
                    q.x = Math.round(Json.integer(entry, "x", 0) / 24f);
                    q.y = Math.round(Json.integer(entry, "y", 0) / 24f);
                    c.quests.add(q);
                    placed.put(qid, true);
                }
                chapters.add(c);
            }
        }
        Chapter rest = new Chapter(uniqueChapterId("Imported"));
        rest.title = "Imported";
        int col = 0;
        for (Map.Entry<Integer, JsonObject> en : byId.entrySet()) {
            if (placed.containsKey(en.getKey())) {
                continue;
            }
            Quest q = quest(en.getValue(), newIds);
            q.x = col % 8 * 2;
            q.y = col / 8 * 2;
            col++;
            rest.quests.add(q);
        }
        if (!rest.quests.isEmpty()) {
            chapters.add(rest);
        }
    }

    private String freshId(Map<Integer, String> used) {
        while (true) {
            String id = Book.randomId(8);
            if (target.quest(id) == null && !used.containsValue(id)) {
                return id;
            }
        }
    }

    private String uniqueChapterId(String title) {
        String base = Book.slug(title);
        if (base.isEmpty()) {
            base = "chapter";
        }
        String id = base;
        int n = 2;
        while (taken(id)) {
            id = base + "_" + n++;
        }
        return id;
    }

    private boolean taken(String id) {
        if (target.chapter(id) != null) {
            return true;
        }
        for (Chapter c : chapters) {
            if (c.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject props(JsonObject o) {
        JsonObject p = Json.obj(o, "properties");
        JsonObject bq = p == null ? null : Json.obj(p, "betterquesting");
        return bq == null ? new JsonObject() : bq;
    }

    private static ItemSpec icon(JsonElement e) {
        if (e == null || !e.isJsonObject()) {
            return ItemSpec.of("minecraft:book");
        }
        return item(e.getAsJsonObject());
    }

    private static ItemSpec item(JsonObject o) {
        return new ItemSpec(Json.str(o, "id", "minecraft:air"), Json.integer(o, "Count", 1), false, null, false);
    }

    private Quest quest(JsonObject src, Map<Integer, String> newIds) {
        Quest q = new Quest(newIds.get(Json.integer(src, "questID", -1)));
        JsonObject props = props(src);
        q.title = Json.str(props, "name", "Quest");
        q.description = Json.str(props, "desc", "").replace("\\n", "\n");
        q.icon = icon(props.get("icon"));
        for (JsonElement e : src.has("preRequisites") && src.get("preRequisites").isJsonArray() ? src.getAsJsonArray("preRequisites") : new JsonArray()) {
            String mapped = newIds.get(e.getAsInt());
            if (mapped != null) {
                q.dependencies.add(mapped);
            }
        }
        int t = 0;
        for (JsonObject task : values(src.get("tasks"))) {
            Def d = task(task, "t" + t);
            if (d != null) {
                q.tasks.add(d);
                t++;
            }
        }
        int r = 0;
        for (JsonObject reward : values(src.get("rewards"))) {
            for (Def d : rewards(reward, r)) {
                q.rewards.add(d);
                r++;
            }
        }
        return q;
    }

    private Def task(JsonObject o, String id) {
        String type = Json.str(o, "taskID", "");
        switch (type) {
            case "bq_standard:retrieval": {
                List<JsonObject> items = values(o.get("requiredItems"));
                if (items.isEmpty()) {
                    return null;
                }
                ItemSpec s = item(items.get(0));
                if (items.size() > 1) {
                    skipped.add("task retrieval with " + items.size() + " items: kept the first");
                }
                return Def.of(id, "item").with("item", s.toString()).with("consume", Json.bool(o, "consume", false));
            }
            case "bq_standard:crafting": {
                List<JsonObject> items = values(o.get("requiredItems"));
                return items.isEmpty() ? null : Def.of(id, "craft").with("item", item(items.get(0)).toString());
            }
            case "bq_standard:checkbox":
                return Def.of(id, "checkmark").with("text", "Check this off");
            case "bq_standard:hunt":
                return Def.of(id, "kill").with("entity", Json.str(o, "target", "")).with("count", Json.lng(o, "required", 1));
            case "bq_standard:location":
                return Def.of(id, "location").with("x", Json.lng(o, "posX", 0)).with("y", Json.lng(o, "posY", 64)).with("z", Json.lng(o, "posZ", 0))
                        .with("radius", Math.max(1, Json.lng(o, "range", 5)));
            case "bq_standard:xp":
                return Def.of(id, "checkmark").with("text", "Reach the required XP, then check this off");
            default:
                skipped.add("task " + type);
                return Def.of(id, "checkmark").with("text", "Imported task (" + type + "): do it, then check it off");
        }
    }

    private List<Def> rewards(JsonObject o, int startIndex) {
        List<Def> out = new ArrayList<>();
        String type = Json.str(o, "rewardID", "");
        switch (type) {
            case "bq_standard:item":
                for (JsonObject it : values(o.get("rewards"))) {
                    out.add(Def.of("r" + (startIndex + out.size()), "item").with("item", item(it).toString()));
                }
                break;
            case "bq_standard:xp":
                out.add(Def.of("r" + startIndex, "xp").with("amount", Json.lng(o, "amount", 1)).with("unit", Json.bool(o, "levels", true) ? "levels" : "points"));
                break;
            case "bq_standard:command":
                out.add(Def.of("r" + startIndex, "command").with("command", Json.str(o, "command", "")));
                break;
            default:
                skipped.add("reward " + type);
        }
        return out;
    }
}

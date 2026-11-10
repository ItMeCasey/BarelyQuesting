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
import java.util.stream.Stream;

/**
 * Converts an FTB Quests folder (the {@code quests} folder with {@code chapters/*.snbt}, or one chapter file) into chapters.
 * Task types map to ours where there is an equivalent: item, kill, advancement, location, dimension, biome, structure, stat,
 * checkmark and xp (as a statistic). Rewards: item, xp, xp_levels, command. Everything else is skipped and counted in
 * {@link #skipped}.
 */
public final class FtbImporter {
    public final List<Chapter> chapters = new ArrayList<>();
    public final List<String> skipped = new ArrayList<>();
    private final Map<String, String> ids = new HashMap<>();
    private final Map<String, String> lang = new HashMap<>();
    private final Book target;

    public FtbImporter(Book target) {
        this.target = target;
    }

    public static boolean looks(Path path) {
        if (Files.isDirectory(path)) {
            return Files.isDirectory(path.resolve("chapters")) && !Files.exists(path.resolve("chapters").resolve("DefaultQuests.json"))
                    && listSnbt(path.resolve("chapters")).size() > 0;
        }
        return path.getFileName().toString().endsWith(".snbt");
    }

    private static List<Path> listSnbt(Path dir) {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".snbt")).sorted().forEach(out::add);
        } catch (IOException e) {
            // empty
        }
        return out;
    }

    public void load(Path path) throws IOException {
        List<Path> files;
        if (Files.isDirectory(path)) {
            files = listSnbt(path.resolve("chapters"));
            Path langFile = path.resolve("lang").resolve("en_us.snbt");
            if (Files.exists(langFile)) {
                JsonElement l = Snbt.parse(Files.readString(langFile, StandardCharsets.UTF_8));
                if (l.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> e : l.getAsJsonObject().entrySet()) {
                        lang.put(e.getKey(), e.getValue().isJsonArray() ? join(e.getValue().getAsJsonArray()) : e.getValue().getAsString());
                    }
                }
            }
        } else {
            files = List.of(path);
        }
        // Chapters sort by order_index, then by file name.
        List<JsonObject> parsed = new ArrayList<>();
        for (Path f : files) {
            JsonElement e = Snbt.parse(Files.readString(f, StandardCharsets.UTF_8));
            if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                if (!o.has("filename")) {
                    String n = f.getFileName().toString();
                    o.addProperty("filename", n.substring(0, n.length() - 5));
                }
                parsed.add(o);
            }
        }
        parsed.sort((a, b) -> Integer.compare(Json.integer(a, "order_index", 0), Json.integer(b, "order_index", 0)));
        // Quest ids first, so dependencies can point across chapters.
        for (JsonObject c : parsed) {
            for (JsonElement q : Json.arr(c, "quests")) {
                ids.put(Json.str(q.getAsJsonObject(), "id", ""), freshId());
            }
        }
        for (JsonObject c : parsed) {
            chapters.add(chapter(c));
        }
    }

    private String freshId() {
        while (true) {
            String id = Book.randomId(8);
            if (target.quest(id) == null && !ids.containsValue(id)) {
                return id;
            }
        }
    }

    private static String join(JsonArray a) {
        StringBuilder sb = new StringBuilder();
        for (JsonElement e : a) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(e.isJsonPrimitive() ? e.getAsString() : "");
        }
        return sb.toString();
    }

    private Chapter chapter(JsonObject o) {
        String rawId = Json.str(o, "id", "");
        String title = plain(Json.str(o, "title", lang.getOrDefault("chapter." + rawId + ".title", Json.str(o, "filename", "Chapter"))));
        Chapter c = new Chapter(uniqueChapterId(title));
        c.title = title;
        c.icon = o.has("icon") ? item(o.get("icon")) : ItemSpec.of("minecraft:book");
        c.group = Json.str(o, "group", "");
        Map<String, int[]> used = new HashMap<>();
        for (JsonElement e : Json.arr(o, "quests")) {
            Quest q = quest(e.getAsJsonObject());
            // Nudge quests that landed on the same grid cell.
            while (used.containsKey(q.x + "," + q.y)) {
                q.x++;
            }
            used.put(q.x + "," + q.y, new int[0]);
            c.quests.add(q);
        }
        return c;
    }

    /** FTB Quests titles carry Minecraft colour and style codes such as "&a" and "&l" and are drawn as plain text here, so they are dropped from titles (descriptions keep theirs for the rich text). */
    static String plain(String text) {
        return text == null ? "" : text.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
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

    private Quest quest(JsonObject o) {
        String rawId = Json.str(o, "id", "");
        Quest q = new Quest(ids.get(rawId));
        q.x = (int) Math.round(o.has("x") ? o.get("x").getAsDouble() : 0);
        q.y = (int) Math.round(o.has("y") ? o.get("y").getAsDouble() : 0);
        q.title = plain(Json.str(o, "title", lang.getOrDefault("quest." + rawId + ".title", "")));
        String desc = lang.get("quest." + rawId + ".quest_desc");
        if (desc == null && o.has("description") && o.get("description").isJsonArray()) {
            desc = join(o.get("description").getAsJsonArray());
        }
        String subtitle = Json.str(o, "subtitle", lang.getOrDefault("quest." + rawId + ".quest_subtitle", ""));
        StringBuilder text = new StringBuilder();
        if (!subtitle.isEmpty()) {
            text.append("*").append(subtitle).append("*\n\n");
        }
        q.description = text + (desc == null ? "" : desc);
        q.optional = Json.bool(o, "optional", false);
        q.hidden = Json.bool(o, "hide", false);
        for (JsonElement d : Json.arr(o, "dependencies")) {
            String mapped = ids.get(d.getAsString());
            if (mapped != null) {
                q.dependencies.add(mapped);
            }
        }
        int t = 0;
        for (JsonElement e : Json.arr(o, "tasks")) {
            Def d = task(e.getAsJsonObject(), "t" + t);
            if (d != null) {
                q.tasks.add(d);
                t++;
            }
        }
        int r = 0;
        for (JsonElement e : Json.arr(o, "rewards")) {
            Def d = reward(e.getAsJsonObject(), "r" + r);
            if (d != null) {
                q.rewards.add(d);
                r++;
            }
        }
        if (o.has("icon")) {
            q.icon = item(o.get("icon"));
        } else if (!q.tasks.isEmpty() && q.tasks.get(0).params.has("item")) {
            q.icon = q.tasks.get(0).item("item").withCount(1);
        } else if (!q.rewards.isEmpty() && q.rewards.get(0).params.has("item")) {
            q.icon = q.rewards.get(0).item("item").withCount(1);
        }
        if (q.title.isEmpty()) {
            q.title = q.icon.id().substring(q.icon.id().indexOf(':') + 1).replace('_', ' ');
        }
        return q;
    }

    /** {@code "minecraft:stone"}, {@code {id: "minecraft:stone", Count: 2b}} or {@code {item: ..., count: 2}}. */
    private ItemSpec item(JsonElement e) {
        if (e.isJsonPrimitive()) {
            return ItemSpec.parse(e.getAsString());
        }
        JsonObject o = e.getAsJsonObject();
        String id = Json.str(o, "id", Json.str(o, "item", "minecraft:air"));
        int count = Json.integer(o, "Count", Json.integer(o, "count", 1));
        String nbt = null;
        JsonObject tag = Json.obj(o, "tag");
        if (tag != null) {
            nbt = tag.toString();
        }
        return new ItemSpec(id, count, false, nbt, false);
    }

    private Def task(JsonObject o, String id) {
        String type = Json.str(o, "type", "");
        switch (type) {
            case "item": {
                ItemSpec spec = item(o.has("item") ? o.get("item") : new com.google.gson.JsonPrimitive("minecraft:air"));
                long count = o.has("count") ? o.get("count").getAsLong() : spec.count();
                Def d = Def.of(id, "item").with("item", spec.withCount((int) Math.max(1, count)).toString());
                d.with("consume", Json.bool(o, "consume_items", false));
                return d;
            }
            case "kill":
                return Def.of(id, "kill").with("entity", Json.str(o, "entity", "")).with("count", Json.lng(o, "value", 1));
            case "advancement":
                return Def.of(id, "advancement").with("advancement", Json.str(o, "advancement", ""));
            case "location": {
                Def d = Def.of(id, "location").with("dimension", Json.str(o, "dimension", ""));
                JsonArray pos = Json.arr(o, "position");
                if (pos.size() >= 3) {
                    d.with("x", pos.get(0).getAsLong()).with("y", pos.get(1).getAsLong()).with("z", pos.get(2).getAsLong());
                }
                JsonArray size = Json.arr(o, "size");
                d.with("radius", size.size() > 0 ? Math.max(1, size.get(0).getAsLong()) : 5);
                return d;
            }
            case "dimension":
                return Def.of(id, "dimension").with("dimension", Json.str(o, "dimension", ""));
            case "biome":
                return Def.of(id, "biome").with("biome", Json.str(o, "biome", ""));
            case "structure":
                return Def.of(id, "structure").with("structure", Json.str(o, "structure", ""));
            case "stat":
                return Def.of(id, "statistic").with("stat_type", "custom").with("stat", Json.str(o, "stat", "")).with("value", Json.lng(o, "value", 1));
            case "checkmark":
                return Def.of(id, "checkmark").with("text", Json.str(o, "title", "Check this off"));
            default:
                skipped.add("task " + type);
                return Def.of(id, "checkmark").with("text", "Imported task (" + type + "): do it, then check it off");
        }
    }

    private Def reward(JsonObject o, String id) {
        String type = Json.str(o, "type", "item");
        switch (type) {
            case "item": {
                ItemSpec spec = item(o.has("item") ? o.get("item") : new com.google.gson.JsonPrimitive("minecraft:air"));
                long count = o.has("count") ? o.get("count").getAsLong() : spec.count();
                return Def.of(id, "item").with("item", spec.withCount((int) Math.max(1, count)).toString());
            }
            case "xp":
                return Def.of(id, "xp").with("amount", Json.lng(o, "xp", 1)).with("unit", "points");
            case "xp_levels":
                return Def.of(id, "xp").with("amount", Json.lng(o, "xp_levels", 1)).with("unit", "levels");
            case "command":
                return Def.of(id, "command").with("command", Json.str(o, "command", ""));
            default:
                skipped.add("reward " + type);
                return null;
        }
    }
}

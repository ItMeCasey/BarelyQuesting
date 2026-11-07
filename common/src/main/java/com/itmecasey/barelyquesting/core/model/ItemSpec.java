package com.itmecasey.barelyquesting.core.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;

import java.util.Objects;

/**
 * An item, a count and optionally a tag and item data, without any Minecraft types. {@code id} is a registry id such as
 * {@code minecraft:stone}; with {@code tag} set it is a tag id ({@code c:ingots/iron}) and matches every item in the tag.
 * {@code data} is the item's NBT (1.20.1) or component patch (newer versions) as SNBT text, compared only when
 * {@code matchData} is set.
 */
public final class ItemSpec {
    public static final ItemSpec EMPTY = new ItemSpec("minecraft:air", 0, false, null, false);

    private final String id;
    private final int count;
    private final boolean tag;
    private final String data;
    private final boolean matchData;

    public ItemSpec(String id, int count, boolean tag, String data, boolean matchData) {
        this.id = id == null ? "minecraft:air" : (id.contains(":") ? id : "minecraft:" + id);
        this.count = Math.max(0, count);
        this.tag = tag;
        this.data = data == null || data.isBlank() ? null : data;
        this.matchData = matchData && this.data != null;
    }

    public static ItemSpec of(String id) {
        return new ItemSpec(id, 1, false, null, false);
    }

    public static ItemSpec of(String id, int count) {
        return new ItemSpec(id, count, false, null, false);
    }

    public String id() {
        return id;
    }

    public int count() {
        return count;
    }

    public boolean isTag() {
        return tag;
    }

    public String data() {
        return data;
    }

    public boolean matchData() {
        return matchData;
    }

    public boolean isEmpty() {
        return count <= 0 || id.equals("minecraft:air");
    }

    public ItemSpec withCount(int newCount) {
        return new ItemSpec(id, newCount, tag, data, matchData);
    }

    public ItemSpec withId(String newId, boolean asTag) {
        return new ItemSpec(newId, count, asTag, data, matchData);
    }

    /** Reads either a bare string ({@code "minecraft:stone"}, {@code "#c:ingots"}, {@code "3 minecraft:stone"}) or an object. */
    public static ItemSpec fromJson(JsonElement e) {
        if (e == null || e.isJsonNull()) {
            return EMPTY;
        }
        if (e.isJsonPrimitive()) {
            return parse(e.getAsString());
        }
        JsonObject o = e.getAsJsonObject();
        String id = Json.str(o, "item", Json.str(o, "id", Json.str(o, "tag", "minecraft:air")));
        boolean tag = o.has("tag") || id.startsWith("#");
        if (id.startsWith("#")) {
            id = id.substring(1);
        }
        return new ItemSpec(id, Json.integer(o, "count", 1), tag, Json.str(o, "data", null), Json.bool(o, "match_data", false));
    }

    /** {@code "3 minecraft:stone"}, {@code "3x minecraft:stone"}, {@code "#c:ingots"}. */
    public static ItemSpec parse(String text) {
        String s = text.trim();
        int count = 1;
        int sp = s.indexOf(' ');
        if (sp > 0) {
            String head = s.substring(0, sp).replace("x", "").replace("X", "");
            try {
                count = Integer.parseInt(head);
                s = s.substring(sp + 1).trim();
            } catch (NumberFormatException ignored) {
                // not a count prefix
            }
        }
        boolean tag = s.startsWith("#");
        if (tag) {
            s = s.substring(1);
        }
        return new ItemSpec(s, count, tag, null, false);
    }

    public JsonElement toJson() {
        if (count == 1 && !tag && data == null) {
            return new com.google.gson.JsonPrimitive(id);
        }
        JsonObject o = new JsonObject();
        o.addProperty(tag ? "tag" : "item", id);
        if (count != 1) {
            o.addProperty("count", count);
        }
        if (data != null) {
            o.addProperty("data", data);
            if (matchData) {
                o.addProperty("match_data", true);
            }
        }
        return o;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemSpec)) {
            return false;
        }
        ItemSpec s = (ItemSpec) o;
        return count == s.count && tag == s.tag && matchData == s.matchData && id.equals(s.id) && Objects.equals(data, s.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, count, tag, data, matchData);
    }

    @Override
    public String toString() {
        return (count == 1 ? "" : count + "x ") + (tag ? "#" : "") + id;
    }
}

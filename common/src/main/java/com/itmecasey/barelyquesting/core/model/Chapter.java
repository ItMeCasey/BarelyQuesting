package com.itmecasey.barelyquesting.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;

import java.util.ArrayList;
import java.util.List;

/** A chapter: a titled group of quests drawn on one graph. */
public final class Chapter {
    public String id;
    public String title = "New Chapter";
    public ItemSpec icon = ItemSpec.of("minecraft:book");
    /** Chapters with the same group name are listed together. */
    public String group = "";
    public final List<Quest> quests = new ArrayList<>();

    public Chapter(String id) {
        this.id = id;
    }

    public Quest quest(String questId) {
        for (Quest q : quests) {
            if (q.id.equals(questId)) {
                return q;
            }
        }
        return null;
    }

    public static Chapter fromJson(JsonObject o) {
        Chapter c = new Chapter(Json.str(o, "id", ""));
        c.title = Json.str(o, "title", c.title);
        if (o.has("icon")) {
            c.icon = ItemSpec.fromJson(o.get("icon"));
        }
        c.group = Json.str(o, "group", "");
        for (JsonElement e : Json.arr(o, "quests")) {
            if (e.isJsonObject()) {
                c.quests.add(Quest.fromJson(e.getAsJsonObject()));
            }
        }
        return c;
    }

    /** The chapter without its quests (used by edit operations and the book file). */
    public JsonObject headerJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("title", title);
        o.add("icon", icon.toJson());
        if (!group.isEmpty()) {
            o.addProperty("group", group);
        }
        return o;
    }

    public JsonObject toJson() {
        JsonObject o = headerJson();
        JsonArray a = new JsonArray();
        for (Quest q : quests) {
            a.add(q.toJson());
        }
        o.add("quests", a);
        return o;
    }
}

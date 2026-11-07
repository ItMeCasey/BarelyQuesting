package com.itmecasey.barelyquesting.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;

import java.util.ArrayList;
import java.util.List;

/** One quest: where it sits on the graph, what it needs (dependencies, locks, tasks) and what it gives (rewards). */
public final class Quest {
    public String id;
    public String title = "New Quest";
    /** Rich text, see {@code ui.RichText}. */
    public String description = "";
    public ItemSpec icon = ItemSpec.of("minecraft:book");
    /** Position on the graph grid, in cells (each cell is one node slot). */
    public int x;
    public int y;
    public final List<String> dependencies = new ArrayList<>();
    public final List<Def> tasks = new ArrayList<>();
    public final List<Def> rewards = new ArrayList<>();
    public final List<Def> locks = new ArrayList<>();
    /** Optional quests never block a chapter from counting as done. */
    public boolean optional;
    /** Hidden quests do not show until all dependencies are done. */
    public boolean hidden;
    /** Complete as soon as any one task is done, instead of all tasks. */
    public boolean anyTask;

    public Quest(String id) {
        this.id = id;
    }

    public Def task(String taskId) {
        for (Def d : tasks) {
            if (d.id.equals(taskId)) {
                return d;
            }
        }
        return null;
    }

    public Def reward(String rewardId) {
        for (Def d : rewards) {
            if (d.id.equals(rewardId)) {
                return d;
            }
        }
        return null;
    }

    public static Quest fromJson(JsonObject o) {
        Quest q = new Quest(Json.str(o, "id", ""));
        q.title = Json.str(o, "title", q.title);
        q.description = Json.str(o, "description", "");
        if (o.has("icon")) {
            q.icon = ItemSpec.fromJson(o.get("icon"));
        }
        q.x = Json.integer(o, "x", 0);
        q.y = Json.integer(o, "y", 0);
        for (JsonElement e : Json.arr(o, "dependencies")) {
            q.dependencies.add(e.getAsString());
        }
        readDefs(Json.arr(o, "tasks"), q.tasks, "t");
        readDefs(Json.arr(o, "rewards"), q.rewards, "r");
        readDefs(Json.arr(o, "locks"), q.locks, "l");
        q.optional = Json.bool(o, "optional", false);
        q.hidden = Json.bool(o, "hidden", false);
        q.anyTask = Json.bool(o, "any_task", false);
        return q;
    }

    private static void readDefs(JsonArray array, List<Def> into, String prefix) {
        int n = 0;
        for (JsonElement e : array) {
            if (e.isJsonObject()) {
                Def d = Def.fromJson(e.getAsJsonObject());
                if (d.id.isEmpty()) {
                    d.id = prefix + n;
                }
                into.add(d);
                n++;
            }
        }
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("title", title);
        if (!description.isEmpty()) {
            o.addProperty("description", description);
        }
        o.add("icon", icon.toJson());
        o.addProperty("x", x);
        o.addProperty("y", y);
        if (!dependencies.isEmpty()) {
            JsonArray a = new JsonArray();
            dependencies.forEach(a::add);
            o.add("dependencies", a);
        }
        writeDefs(o, "tasks", tasks);
        writeDefs(o, "rewards", rewards);
        writeDefs(o, "locks", locks);
        if (optional) {
            o.addProperty("optional", true);
        }
        if (hidden) {
            o.addProperty("hidden", true);
        }
        if (anyTask) {
            o.addProperty("any_task", true);
        }
        return o;
    }

    private static void writeDefs(JsonObject o, String key, List<Def> defs) {
        if (defs.isEmpty()) {
            return;
        }
        JsonArray a = new JsonArray();
        for (Def d : defs) {
            a.add(d.toJson());
        }
        o.add(key, a);
    }

    public Quest copy() {
        return fromJson(toJson());
    }
}

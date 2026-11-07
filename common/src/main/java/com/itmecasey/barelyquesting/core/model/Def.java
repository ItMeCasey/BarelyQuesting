package com.itmecasey.barelyquesting.core.model;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;

/**
 * A task, reward or lock definition: an id (unique inside its quest), a type name and free-form parameters. The behavior of the
 * type is looked up in the matching registry, so scripts can add new types without changing the model.
 */
public final class Def {
    public String id;
    public String type;
    public JsonObject params;

    public Def(String id, String type, JsonObject params) {
        this.id = id;
        this.type = type;
        this.params = params == null ? new JsonObject() : params;
    }

    public static Def of(String id, String type) {
        return new Def(id, type, new JsonObject());
    }

    public Def with(String key, String value) {
        params.addProperty(key, value);
        return this;
    }

    public Def with(String key, Number value) {
        params.addProperty(key, value);
        return this;
    }

    public Def with(String key, boolean value) {
        params.addProperty(key, value);
        return this;
    }

    public String str(String key, String def) {
        return Json.str(params, key, def);
    }

    public long num(String key, long def) {
        return Json.lng(params, key, def);
    }

    public boolean bool(String key, boolean def) {
        return Json.bool(params, key, def);
    }

    public ItemSpec item(String key) {
        return ItemSpec.fromJson(params.get(key));
    }

    public static Def fromJson(JsonObject o) {
        JsonObject params = o.deepCopy();
        params.remove("id");
        params.remove("type");
        return new Def(Json.str(o, "id", ""), Json.str(o, "type", ""), params);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("type", type);
        for (String k : params.keySet()) {
            o.add(k, params.get(k));
        }
        return o;
    }

    public Def copy() {
        return new Def(id, type, params.deepCopy());
    }

    @Override
    public String toString() {
        return type + ":" + id;
    }
}

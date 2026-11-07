package com.itmecasey.barelyquesting.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Small JSON helpers on top of Gson: parsing, stable pretty printing, atomic file writes and defaulted getters. */
public final class Json {
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private Json() {
    }

    public static JsonElement parse(String text) {
        return JsonParser.parseString(text);
    }

    public static JsonObject parseObject(String text) {
        JsonElement e = JsonParser.parseString(text);
        if (!e.isJsonObject()) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        return e.getAsJsonObject();
    }

    public static String pretty(JsonElement e) {
        return PRETTY.toJson(e) + "\n";
    }

    public static String compact(JsonElement e) {
        return COMPACT.toJson(e);
    }

    /** Writes through a temporary file and a move, so a crash never leaves half a file. */
    public static void writeAtomic(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, text, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String str(JsonObject o, String key, String def) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    public static int integer(JsonObject o, String key, int def) {
        JsonElement e = o == null ? null : o.get(key);
        try {
            return e != null && e.isJsonPrimitive() ? e.getAsInt() : def;
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public static long lng(JsonObject o, String key, long def) {
        JsonElement e = o == null ? null : o.get(key);
        try {
            return e != null && e.isJsonPrimitive() ? e.getAsLong() : def;
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
    }

    public static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    public static JsonArray arr(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
    }

    public static JsonObject deepCopy(JsonObject o) {
        return o.deepCopy();
    }
}

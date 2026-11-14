package com.itmecasey.barelyquesting.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The current values of the settings in {@link ConfigKeys}. The loader's config spec pushes its values in with {@link #load} when a
 * .toml file loads or reloads; code that changes a setting at runtime (the editor list, the theme) calls {@link #set} and the loader
 * glue writes it back to the file through the writer. Without a loader (unit tests) the values are plain defaults.
 */
public final class Settings {
    /** Writes changed values to the loader's config file. */
    public interface Writer {
        void write(Map<String, Object> changes);
    }

    private static final Map<String, Object> VALUES = new ConcurrentHashMap<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Writer writer;

    static {
        reset();
    }

    private Settings() {
    }

    public static void reset() {
        VALUES.clear();
        for (ConfigKeys.Key k : ConfigKeys.ALL) {
            VALUES.put(k.name(), k.def());
        }
    }

    /** Installs the loader's file writer (null removes it). */
    public static void writer(Writer w) {
        writer = w;
    }

    /** Called after a config file loaded or reloaded and its values were pushed in. */
    public static void onLoaded(Runnable listener) {
        LISTENERS.add(listener);
    }

    /** Takes values from a loaded config file (only the keys present; a value of the wrong type is ignored). */
    public static void load(Map<String, ?> values) {
        for (Map.Entry<String, ?> e : values.entrySet()) {
            put(e.getKey(), e.getValue());
        }
        for (Runnable r : LISTENERS) {
            r.run();
        }
    }

    /** Changes values at runtime and writes them to the config file. */
    public static void set(Map<String, Object> changes) {
        for (Map.Entry<String, Object> e : changes.entrySet()) {
            put(e.getKey(), e.getValue());
        }
        Writer w = writer;
        if (w != null) {
            w.write(changes);
        }
    }

    public static void set(String key, Object value) {
        set(Map.of(key, value));
    }

    private static void put(String key, Object value) {
        ConfigKeys.Key k;
        try {
            k = ConfigKeys.key(key);
        } catch (IllegalArgumentException e) {
            return;
        }
        switch (k.type()) {
            case BOOL:
                if (value instanceof Boolean) {
                    VALUES.put(key, value);
                }
                break;
            case STRING:
                if (value instanceof String) {
                    VALUES.put(key, value);
                }
                break;
            case CHOICE:
                if (value instanceof String && k.choices().contains(value)) {
                    VALUES.put(key, value);
                }
                break;
            case LIST:
                if (value instanceof List) {
                    List<String> out = new ArrayList<>();
                    for (Object o : (List<?>) value) {
                        if (o instanceof String) {
                            out.add((String) o);
                        }
                    }
                    VALUES.put(key, List.copyOf(out));
                }
                break;
            default:
                break;
        }
    }

    public static boolean bool(String key) {
        return Boolean.TRUE.equals(VALUES.get(key));
    }

    public static String str(String key) {
        Object v = VALUES.get(key);
        return v instanceof String ? (String) v : "";
    }

    @SuppressWarnings("unchecked")
    public static List<String> list(String key) {
        Object v = VALUES.get(key);
        return v instanceof List ? (List<String>) v : List.of();
    }

    // ---- named accessors for the server settings ----

    public static boolean editing() {
        return bool("editing");
    }

    public static boolean creativeEdits() {
        return bool("creative_edits");
    }

    /** Whether the name (any case) is on the editor list. */
    public static boolean isEditor(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String s : list("editors")) {
            if (s.toLowerCase(Locale.ROOT).equals(lower)) {
                return true;
            }
        }
        return false;
    }

    public static boolean giveBookOnJoin() {
        return bool("give_book_on_join");
    }

    public static boolean openBookOnJoin() {
        return bool("open_book_on_join");
    }
}

package com.itmecasey.barelyquesting.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.ui.ClientEnv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds and holds the themes. {@code assets/<ns>/barelyquesting/themes/index.json} lists theme ids, and each id has a file
 * {@code <id>.json} next to it, so any resource pack can add themes. {@link #reload()} runs on every resource reload (F3+T).
 */
public final class ThemeManager {
    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();
    private static String selected = "barelyquesting:default";

    private ThemeManager() {
    }

    public static synchronized void reload() {
        ClientEnv env = ClientEnv.Holder.get();
        Map<String, Theme> found = new LinkedHashMap<>();
        Map<String, String> indexes = env.themeFiles("index.json");
        List<String> namespaces = new ArrayList<>(indexes.keySet());
        namespaces.sort((a, b) -> a.equals("barelyquesting") ? -1 : b.equals("barelyquesting") ? 1 : a.compareTo(b));
        for (String ns : namespaces) {
            try {
                JsonObject index = Json.parseObject(indexes.get(ns));
                for (JsonElement e : Json.arr(index, "themes")) {
                    String name = e.getAsString();
                    String text = themeText(env, ns, name);
                    if (text == null) {
                        continue;
                    }
                    String id = ns + ":" + name;
                    found.put(id, Theme.fromJson(id, Json.parseObject(text)));
                }
            } catch (RuntimeException ex) {
                java.util.logging.Logger.getLogger("barelyquesting").warning("Bad theme index in " + ns + ": " + ex.getMessage());
            }
        }
        if (found.isEmpty()) {
            found.put("barelyquesting:default", Theme.builtin());
        }
        THEMES.clear();
        THEMES.putAll(found);
        if (!THEMES.containsKey(selected)) {
            selected = THEMES.keySet().iterator().next();
        }
    }

    private static String themeText(ClientEnv env, String ns, String name) {
        // themeFiles(name + ".json") returns one entry per namespace; take this namespace's.
        return env.themeFiles(name + ".json").get(ns);
    }

    public static synchronized Theme current() {
        Theme t = THEMES.get(selected);
        if (t == null) {
            if (THEMES.isEmpty()) {
                THEMES.put("barelyquesting:default", Theme.builtin());
            }
            t = THEMES.values().iterator().next();
        }
        return t;
    }

    public static synchronized List<Theme> all() {
        if (THEMES.isEmpty()) {
            THEMES.put("barelyquesting:default", Theme.builtin());
        }
        return new ArrayList<>(THEMES.values());
    }

    public static synchronized void select(String id) {
        if (THEMES.containsKey(id)) {
            selected = id;
        }
    }

    public static synchronized String selectedId() {
        return selected;
    }

    /** Test hook: put a theme in the registry. */
    public static synchronized void put(Theme t) {
        THEMES.put(t.id, t);
    }
}

package com.itmecasey.barelyquesting.ui;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.model.Def;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Which parameters each task, reward and lock type has, so the editor can show a field for each. */
public final class Schemas {
    public enum Kind { TEXT, NUMBER, BOOL, ITEM, REGISTRY, CHOICE, QUEST }

    /** One editable parameter. */
    public static final class Param {
        public final String key;
        public final String label;
        public final Kind kind;
        /** Registry kind for REGISTRY, comma separated options for CHOICE. */
        public final String ref;
        public final String def;

        Param(String key, String label, Kind kind, String ref, String def) {
            this.key = key;
            this.label = label;
            this.kind = kind;
            this.ref = ref;
            this.def = def;
        }
    }

    private Schemas() {
    }

    private static Param p(String key, String label, Kind kind, String ref, String def) {
        return new Param(key, label, kind, ref, def);
    }

    public static List<Param> task(String type) {
        switch (type) {
            case "item":
                return Arrays.asList(p("item", "Item", Kind.ITEM, "", "minecraft:stone"), p("consume", "Submit (take items)", Kind.BOOL, "", "false"));
            case "craft":
            case "obtain":
                return Arrays.asList(p("item", "Item", Kind.ITEM, "", "minecraft:stone"));
            case "kill":
                return Arrays.asList(p("entity", "Entity", Kind.REGISTRY, "entity", "minecraft:zombie"), p("count", "Count", Kind.NUMBER, "", "1"));
            case "advancement":
                return Arrays.asList(p("advancement", "Advancement", Kind.REGISTRY, "advancement", "minecraft:story/root"));
            case "statistic":
                return Arrays.asList(p("stat_type", "Type", Kind.CHOICE, "custom,mined,crafted,used,broken,picked_up,dropped,killed,killed_by", "custom"),
                        p("stat", "Statistic", Kind.TEXT, "", "minecraft:jump"), p("value", "Value", Kind.NUMBER, "", "10"));
            case "location":
                return Arrays.asList(p("x", "X", Kind.NUMBER, "", "0"), p("y", "Y", Kind.NUMBER, "", "64"), p("z", "Z", Kind.NUMBER, "", "0"),
                        p("radius", "Radius", Kind.NUMBER, "", "5"), p("dimension", "Dimension (optional)", Kind.REGISTRY, "dimension", ""));
            case "dimension":
                return Arrays.asList(p("dimension", "Dimension", Kind.REGISTRY, "dimension", "minecraft:the_nether"));
            case "biome":
                return Arrays.asList(p("biome", "Biome", Kind.REGISTRY, "biome", "minecraft:forest"));
            case "structure":
                return Arrays.asList(p("structure", "Structure", Kind.REGISTRY, "structure", "minecraft:village_plains"));
            case "checkmark":
                return Arrays.asList(p("text", "Text", Kind.TEXT, "", "Check this off"));
            case "scripted":
                return Arrays.asList(p("name", "Script name", Kind.TEXT, "", ""), p("count", "Count", Kind.NUMBER, "", "1"), p("text", "Text", Kind.TEXT, "", ""));
            default:
                return new ArrayList<>();
        }
    }

    public static List<Param> reward(String type) {
        List<Param> list = new ArrayList<>();
        switch (type) {
            case "item":
                list.add(p("item", "Item", Kind.ITEM, "", "minecraft:apple"));
                break;
            case "xp":
                list.add(p("amount", "Amount", Kind.NUMBER, "", "10"));
                list.add(p("unit", "Unit", Kind.CHOICE, "points,levels", "points"));
                break;
            case "command":
                list.add(p("command", "Command ({player})", Kind.TEXT, "", "say Well done {player}"));
                break;
            case "scripted":
                list.add(p("name", "Script name", Kind.TEXT, "", ""));
                list.add(p("text", "Text", Kind.TEXT, "", ""));
                break;
            default:
                break;
        }
        list.add(p("scope", "Given to", Kind.CHOICE, "player,team", "player"));
        list.add(p("auto", "Claim automatically", Kind.BOOL, "", "false"));
        return list;
    }

    public static List<Param> lock(String type) {
        switch (type) {
            case "quest":
                return Arrays.asList(p("quest", "Quest", Kind.QUEST, "", ""));
            case "time":
                return Arrays.asList(p("after", "Opens at (2026-12-01 or ISO)", Kind.TEXT, "", ""), p("before", "Closes at", Kind.TEXT, "", ""));
            case "script":
                return Arrays.asList(p("name", "Script name", Kind.TEXT, "", ""));
            default:
                return new ArrayList<>();
        }
    }

    /** Fills in the default parameters for a fresh task, reward or lock of a type. */
    public static void applyDefaults(Def d, List<Param> params) {
        for (Param p : params) {
            if (p.def.isEmpty() || d.params.has(p.key)) {
                continue;
            }
            switch (p.kind) {
                case NUMBER:
                    d.params.addProperty(p.key, Long.parseLong(p.def));
                    break;
                case BOOL:
                    d.params.addProperty(p.key, Boolean.parseBoolean(p.def));
                    break;
                default:
                    d.params.addProperty(p.key, p.def);
            }
        }
    }

    /** Parameters of a def whose type has no schema (added by scripts): every existing key as text. */
    public static List<Param> generic(JsonObject params) {
        List<Param> out = new ArrayList<>();
        for (String k : params.keySet()) {
            out.add(p(k, k, Kind.TEXT, "", ""));
        }
        return out;
    }
}

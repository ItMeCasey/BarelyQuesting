package com.itmecasey.barelyquesting.core.config;

import java.util.ArrayList;
import java.util.List;

/**
 * The one list of Barely Questing's settings: key, default, allowed values and description. The Forge and NeoForge config specs are
 * built from it, the migration writes its TOML files from it, and {@link Settings} holds the values it describes.
 */
public final class ConfigKeys {
    /** COMMON values are read by the server (and by a singleplayer game); CLIENT values only by the player's own game. */
    public enum Side { COMMON, CLIENT }

    public enum Type { BOOL, STRING, LIST, CHOICE }

    /** One setting. {@code def} is a Boolean, a String or a List of Strings; {@code choices} is only used by CHOICE. */
    public record Key(String name, Side side, Type type, Object def, List<String> choices, String comment) {
    }

    public static final List<Key> ALL = new ArrayList<>();

    static {
        add("editing", Side.COMMON, Type.BOOL, true,
                "Set to false to turn editing off for everyone, operators included: the Edit button is hidden and every edit is refused.",
                "Scripts, /barelyquesting import and /barelyquesting reload keep working, so a pack author still changes the book by editing the quest files.");
        add("creative_edits", Side.COMMON, Type.BOOL, true,
                "Whether players in creative mode may edit the book (while editing is on).");
        add("editors", Side.COMMON, Type.LIST, List.<String>of(),
                "Player names allowed to edit besides operators and creative players. /barelyquesting editor add and remove change this list.");
        add("give_book_on_join", Side.COMMON, Type.BOOL, false,
                "Give each player one Quest Book the first time they join a world.");
        add("open_book_on_join", Side.COMMON, Type.BOOL, false,
                "Open the book the first time a player joins a world.");
        add("hud_tracker", Side.CLIENT, Type.BOOL, true,
                "Show the pinned-quest tracker in the corner of the game screen.");
        add("theme", Side.CLIENT, Type.STRING, "",
                "Id of the book theme, for example barelyquesting:parchment. Empty uses the default theme.");
        addChoice("view", Side.CLIENT, "graph", List.of("graph", "list"),
                "How a chapter is shown: as a node graph or as a list.");
        add("last_chapter", Side.CLIENT, Type.STRING, "",
                "The chapter the book was last open on; the book reopens there.");
        add("pinned_quests", Side.CLIENT, Type.LIST, List.<String>of(),
                "Quest ids pinned to the HUD tracker.");
    }

    private ConfigKeys() {
    }

    private static void add(String name, Side side, Type type, Object def, String... comment) {
        ALL.add(new Key(name, side, type, def, List.of(), String.join(" ", comment)));
    }

    private static void addChoice(String name, Side side, String def, List<String> choices, String comment) {
        ALL.add(new Key(name, side, Type.CHOICE, def, choices, comment));
    }

    public static List<Key> keys(Side side) {
        List<Key> out = new ArrayList<>();
        for (Key k : ALL) {
            if (k.side() == side) {
                out.add(k);
            }
        }
        return out;
    }

    public static Key key(String name) {
        for (Key k : ALL) {
            if (k.name().equals(name)) {
                return k;
            }
        }
        throw new IllegalArgumentException("unknown setting " + name);
    }
}

package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.core.model.ItemSpec;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/** What the client-side code needs from the game that is not drawing. The version module installs one at startup. */
public interface ClientEnv {
    /** Every registered item id, sorted. */
    List<String> itemIds();

    /** Registry ids of one kind: {@code entity}, {@code biome}, {@code structure}, {@code advancement}, {@code dimension}, {@code stat}. */
    List<String> ids(String kind);

    /** Sends a message to the server (no-op when not connected). */
    void sendToServer(byte[] message);

    /** The mod's config folder. */
    Path configDir();

    /** Opens recipe pages for the item in the installed viewer; false when there is none. */
    boolean showRecipes(ItemSpec item);

    /** Opens usage pages for the item in the installed viewer; false when there is none. */
    boolean showUses(ItemSpec item);

    /** Names of installed viewers, for the settings page (empty when none). */
    default List<String> viewers() {
        return Collections.emptyList();
    }

    /** Contents of every {@code <namespace>:barelyquesting/themes/<name>} file of loaded resource packs, keyed by namespace. */
    java.util.Map<String, String> themeFiles(String name);

    /** Game time in ticks for animations; real time is fine. */
    default long millis() {
        return System.currentTimeMillis();
    }

    /** Opens a link in the browser (after the game's own confirmation where it has one). */
    default void openUrl(String url) {
    }

    /** The player's own name, or empty. */
    default String playerName() {
        return "";
    }

    /** A stand-in used before the game installs the real one, and in tests. */
    ClientEnv NONE = new ClientEnv() {
        @Override
        public List<String> itemIds() {
            return Collections.emptyList();
        }

        @Override
        public List<String> ids(String kind) {
            return Collections.emptyList();
        }

        @Override
        public void sendToServer(byte[] message) {
        }

        @Override
        public Path configDir() {
            return Path.of("config", "barelyquesting");
        }

        @Override
        public boolean showRecipes(ItemSpec item) {
            return false;
        }

        @Override
        public boolean showUses(ItemSpec item) {
            return false;
        }

        @Override
        public java.util.Map<String, String> themeFiles(String name) {
            return Collections.emptyMap();
        }
    };

    /** Holder for the installed environment. */
    final class Holder {
        private static volatile ClientEnv env = NONE;

        private Holder() {
        }

        public static ClientEnv get() {
            return env;
        }

        public static void set(ClientEnv e) {
            env = e;
        }
    }
}

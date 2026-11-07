package com.itmecasey.barelyquesting.core.script;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * KubeJS support without a compile-time dependency on any KubeJS version: scripts reach the {@link QuestApi} through KubeJS's own
 * {@code Java.loadClass}. When KubeJS is installed, a small startup script is written once to
 * {@code kubejs/startup_scripts/barelyquesting.js}; it puts the API in {@code global.BarelyQuesting} so every script type can use it.
 * Delete the file to opt out. KubeJS is optional; without it nothing happens.
 */
public final class KubeJsBridge {
    private static final Logger LOG = Logger.getLogger("barelyquesting");

    static final String SCRIPT = "// Written by Barely Questing. Delete this file to remove the BarelyQuesting global.\n"
            + "// Use it from any script type: BarelyQuesting.chapter(...), BarelyQuesting.quest(...), BarelyQuesting.registerTask(...)\n"
            + "// Functions are plain JavaScript functions. See https://itmecasey.com/wiki/barelyquesting/ for the full list.\n"
            + "global.BarelyQuesting = new (Java.loadClass('com.itmecasey.barelyquesting.core.script.QuestApi'))()\n";

    private KubeJsBridge() {
    }

    /** True when a KubeJS class is visible. */
    public static boolean present() {
        for (String name : new String[] {"dev.latvian.mods.kubejs.KubeJS", "dev.latvian.mods.kubejs.KubeJSPlugin"}) {
            try {
                Class.forName(name);
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                // try the next name
            }
        }
        return false;
    }

    /** Writes the helper script into the game folder's kubejs when KubeJS is installed; returns whether KubeJS was found. */
    public static boolean install(Path gameDir) {
        if (!present()) {
            return false;
        }
        Path file = gameDir.resolve("kubejs").resolve("startup_scripts").resolve("barelyquesting.js");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, SCRIPT, StandardCharsets.UTF_8);
                LOG.info("Barely Questing: wrote " + file + " so KubeJS scripts get the BarelyQuesting global");
            }
        } catch (IOException e) {
            LOG.warning("Barely Questing: could not write the KubeJS helper script: " + e);
        }
        return true;
    }
}

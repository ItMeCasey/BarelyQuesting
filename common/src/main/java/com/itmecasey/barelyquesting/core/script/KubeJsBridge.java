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
 * Existing helper files are preserved. KubeJS is optional; without it nothing happens.
 */
public final class KubeJsBridge {
    private static final Logger LOG = Logger.getLogger("barelyquesting");

    static final String SCRIPT = "// Written by Barely Questing. Existing files are never overwritten.\n"
            + "// Use global.BarelyQuesting from server scripts; register callbacks in startup scripts.\n"
            + "// Functions are plain JavaScript functions. See docs/scripting.md in the Barely Questing source for examples.\n"
            + "global.BarelyQuesting = Java.loadClass('com.itmecasey.barelyquesting.core.script.Bridges').API\n";

    private KubeJsBridge() {
    }

    /** True when a KubeJS class is visible. */
    public static boolean present() {
        for (String name : new String[] {"dev.latvian.mods.kubejs.KubeJS", "dev.latvian.mods.kubejs.KubeJSPlugin"}) {
            try {
                Class.forName(name, false, KubeJsBridge.class.getClassLoader());
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
        return writeHelper(gameDir);
    }

    static boolean writeHelper(Path gameDir) {
        Path file = gameDir.resolve("kubejs").resolve("startup_scripts").resolve("barelyquesting.js");
        try {
            if (Files.exists(file) && !Files.isRegularFile(file)) return false;
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, SCRIPT, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW);
                LOG.info("Barely Questing: wrote " + file + " so KubeJS scripts get the BarelyQuesting global");
            }
        } catch (java.nio.file.FileAlreadyExistsException e) {
            return Files.isRegularFile(file);
        } catch (IOException e) {
            LOG.warning("Barely Questing: could not write the KubeJS helper script: " + e);
            return false;
        }
        return true;
    }
}

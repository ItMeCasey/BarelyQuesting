package com.itmecasey.barelyquesting.core.script;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class KubeJsBridgeTest {
    @TempDir Path dir;
    @Test void helperUsesSharedApiAndExplicitGlobal() throws Exception {
        assertTrue(KubeJsBridge.writeHelper(dir));
        String script = Files.readString(dir.resolve("kubejs/startup_scripts/barelyquesting.js"));
        assertTrue(script.contains("global.BarelyQuesting = Java.loadClass"));
        assertTrue(script.contains("Bridges').API"));
    }
    @Test void existingUserHelperIsPreserved() throws Exception {
        Path file = dir.resolve("kubejs/startup_scripts/barelyquesting.js");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "// custom helper");
        assertTrue(KubeJsBridge.writeHelper(dir));
        assertEquals("// custom helper", Files.readString(file));
    }
    @Test void unwritableHelperPathReportsFailure() throws Exception {
        Files.writeString(dir.resolve("kubejs"), "a file blocks the directory");
        assertFalse(KubeJsBridge.writeHelper(dir));
    }
    @Test void helperDirectoryIsNotMistakenForAnInstalledScript() throws Exception {
        Files.createDirectories(dir.resolve("kubejs/startup_scripts/barelyquesting.js"));
        assertFalse(KubeJsBridge.writeHelper(dir));
    }
    @Test void absentKubeJsDoesNotCreateDirectories() {
        assertFalse(KubeJsBridge.install(dir));
        assertFalse(Files.exists(dir.resolve("kubejs")));
    }
}

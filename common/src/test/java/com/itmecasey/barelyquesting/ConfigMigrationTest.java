package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelyquesting.core.config.ConfigKeys;
import com.itmecasey.barelyquesting.core.config.ConfigMigration;
import com.itmecasey.barelyquesting.core.config.Settings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The settings of released versions (book.json and client.json before 2026.11.9) must land in the loader's toml files with the values the
 * user set. The fixtures under config-migration/ are files written by real builds of 2026.11.1 and 2026.11.7, plus the same files edited the
 * way a user edits them.
 */
class ConfigMigrationTest {
    @TempDir
    Path dir;

    @AfterEach
    void reset() {
        Settings.reset();
    }

    private byte[] fixture(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/config-migration/" + path)) {
            return in.readAllBytes();
        }
    }

    private void put(String file, byte[] bytes) throws IOException {
        Path f = dir.resolve("barelyquesting").resolve(file);
        Files.createDirectories(f.getParent());
        Files.write(f, bytes);
    }

    private void put(String file, String text) throws IOException {
        put(file, text.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> common() throws IOException {
        return readToml(dir.resolve("barelyquesting-common.toml"));
    }

    private Map<String, Object> client() throws IOException {
        return readToml(dir.resolve("barelyquesting-client.toml"));
    }

    // ---- real files from released versions ----

    @Test
    void realFilesFrom20261101() throws IOException {
        byte[] book = fixture("v2026.11.1/book.json");
        byte[] client = fixture("v2026.11.1/client.json");
        put("book.json", book);
        put("client.json", client);
        ConfigMigration.Result r = ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertEquals(2, r.written.size());
        Map<String, Object> c = common();
        assertEquals(true, c.get("editing"));
        assertEquals(true, c.get("creative_edits"));
        assertEquals(List.of(), c.get("editors"));
        assertEquals(false, c.get("give_book_on_join"));
        assertEquals(false, c.get("open_book_on_join"));
        Map<String, Object> k = client();
        assertEquals(true, k.get("hud_tracker"));
        assertEquals("barelyquesting:default", k.get("theme"));
        assertEquals("graph", k.get("view"));
        assertEquals("getting_started", k.get("last_chapter"));
        assertEquals(List.of("pickaxe"), k.get("pinned_quests"));
        // client.json moved to client.json.back: a header line, then the exact original bytes
        assertFalse(Files.exists(dir.resolve("barelyquesting/client.json")));
        assertBackup(dir.resolve("barelyquesting/client.json.back"), client, "config/barelyquesting-client.toml");
        // book.json also holds the quest book's title and chapter order, so it stays, without the setting keys
        assertBackup(dir.resolve("barelyquesting/book.json.back"), book, "config/barelyquesting-common.toml");
        String kept = Files.readString(dir.resolve("barelyquesting/book.json"));
        assertTrue(kept.contains("\"title\"") && kept.contains("\"chapters\"") && kept.contains("getting_started"), kept);
        assertFalse(kept.contains("creative_edits") || kept.contains("editors"), kept);
        assertEquals(2, r.backups.size());
        // a second launch does nothing
        ConfigMigration.Result again = ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertTrue(again.written.isEmpty() && again.backups.isEmpty() && again.log.isEmpty());
        assertFalse(Files.exists(dir.resolve("barelyquesting/book.json.back2")));
    }

    private void assertBackup(Path backup, byte[] original, String tomlName) throws IOException {
        byte[] all = Files.readAllBytes(backup);
        int nlAt = 0;
        while (all[nlAt] != '\n') {
            nlAt++;
        }
        String header = new String(all, 0, nlAt, StandardCharsets.UTF_8);
        assertTrue(header.startsWith("Migrated by Barely Questing "), header);
        assertTrue(header.contains(" on " + java.time.LocalDate.now() + " to " + tomlName + "; this file is no longer read. Kept as a backup."), header);
        byte[] rest = java.util.Arrays.copyOfRange(all, nlAt + 1, all.length);
        assertArrayEquals(original, rest, "the backup holds the exact original bytes");
    }

    @Test
    void anExistingBackupIsNeverOverwritten() throws IOException {
        put("client.json", "{\"hud\": false}");
        put("client.json.back", "an older backup");
        put("client.json.back2", "another older backup");
        ConfigMigration.Result r = ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertEquals("an older backup", Files.readString(dir.resolve("barelyquesting/client.json.back")));
        assertEquals("another older backup", Files.readString(dir.resolve("barelyquesting/client.json.back2")));
        assertBackup(dir.resolve("barelyquesting/client.json.back3"), "{\"hud\": false}".getBytes(StandardCharsets.UTF_8), "config/barelyquesting-client.toml");
        assertFalse(Files.exists(dir.resolve("barelyquesting/client.json")));
        assertEquals(false, client().get("hud_tracker"));
        assertEquals(1, r.backups.size());
    }

    @Test
    void realFilesFrom20261107() throws IOException {
        put("book.json", fixture("v2026.11.7/book.json"));
        put("client.json", fixture("v2026.11.7/client.json"));
        ConfigMigration.run(dir, "barelyquesting-common.toml");
        Map<String, Object> c = common();
        assertEquals(true, c.get("editing"));
        assertEquals(true, c.get("creative_edits"));
        assertEquals(false, c.get("give_book_on_join"));
        assertEquals(false, c.get("open_book_on_join"));
        assertEquals("graph", client().get("view"));
    }

    @Test
    void everyValueAUserSetLandsInTheToml() throws IOException {
        // the 2026.11.7 files as a user edits them: every setting changed
        String book = new String(fixture("v2026.11.7/book.json"), StandardCharsets.UTF_8)
                .replace("\"creative_edits\": true", "\"creative_edits\": false")
                .replace("\"editing\": \"on\"", "\"editing\": \"off\"")
                .replace("\"give_book_on_join\": false", "\"give_book_on_join\": true")
                .replace("\"open_book_on_join\": false", "\"open_book_on_join\": true")
                .replace("\"editors\": []", "\"editors\": [\"casey\", \"Bob_2\"]");
        String client = new String(fixture("v2026.11.7/client.json"), StandardCharsets.UTF_8)
                .replace("\"view\": \"graph\"", "\"view\": \"list\"")
                .replace("\"chapter\": \"getting_started\"", "\"chapter\": \"exploring\"")
                .replace("\"hud\": true", "\"hud\": false")
                .replace("barelyquesting:default", "barelyquesting:parchment")
                .replace("\"pickaxe\"", "\"pickaxe\", \"nether\"");
        put("book.json", book);
        put("client.json", client);
        ConfigMigration.run(dir, "barelyquesting-common.toml");
        Map<String, Object> c = common();
        assertEquals(false, c.get("creative_edits"));
        assertEquals(false, c.get("editing"));
        assertEquals(true, c.get("give_book_on_join"));
        assertEquals(true, c.get("open_book_on_join"));
        assertEquals(List.of("casey", "Bob_2"), c.get("editors"));
        Map<String, Object> k = client();
        assertEquals("list", k.get("view"));
        assertEquals("exploring", k.get("last_chapter"));
        assertEquals(false, k.get("hud_tracker"));
        assertEquals("barelyquesting:parchment", k.get("theme"));
        assertEquals(List.of("pickaxe", "nether"), k.get("pinned_quests"));
    }

    @Test
    void a20261101BookWithCreativeEditsOffAndEditors() throws IOException {
        String book = new String(fixture("v2026.11.1/book.json"), StandardCharsets.UTF_8)
                .replace("\"creative_edits\": true", "\"creative_edits\": false")
                .replace("\"editors\": []", "\"editors\": [\"alice\"]");
        put("book.json", book);
        ConfigMigration.run(dir, "barelyquesting-common.toml");
        Map<String, Object> c = common();
        assertEquals(false, c.get("creative_edits"));
        assertEquals(List.of("alice"), c.get("editors"));
        assertEquals(true, c.get("editing"), "a version before the switch existed keeps editing on");
        assertFalse(Files.exists(dir.resolve("barelyquesting-client.toml")), "no client.json, so no client toml is made");
    }

    // ---- missing, empty, corrupt, partial ----

    @Test
    void aBookFileWithoutSettingKeysIsLeftAlone() throws IOException {
        String text = "{\"format\": 1, \"title\": \"Quest Book\", \"chapters\": [\"a\"]}";
        put("book.json", text);
        ConfigMigration.Result r = ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertTrue(r.written.isEmpty() && r.backups.isEmpty());
        assertFalse(Files.exists(dir.resolve("barelyquesting-common.toml")));
        assertEquals(text, Files.readString(dir.resolve("barelyquesting/book.json")));
    }

    @Test
    void noOldFilesWritesNothing() throws IOException {
        ConfigMigration.Result r = ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertTrue(r.written.isEmpty());
        assertFalse(Files.exists(dir.resolve("barelyquesting-common.toml")));
    }

    @Test
    void anExistingTomlIsNeverOverwritten() throws IOException {
        put("book.json", "{\"creative_edits\": false}");
        Files.writeString(dir.resolve("barelyquesting-common.toml"), "creative_edits = true\n");
        ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertEquals("creative_edits = true\n", Files.readString(dir.resolve("barelyquesting-common.toml")));
    }

    @Test
    void anEmptyOldFileGivesDefaults() throws IOException {
        put("book.json", "");
        put("client.json", "   \n");
        ConfigMigration.Result r = ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertEquals(2, r.written.size());
        assertEquals(defaults(ConfigKeys.Side.COMMON), common());
        assertEquals(defaults(ConfigKeys.Side.CLIENT), client());
        assertTrue(r.log.stream().anyMatch(l -> l.contains("empty")), r.log.toString());
        assertTrue(Files.exists(dir.resolve("barelyquesting/book.json")) && Files.exists(dir.resolve("barelyquesting/client.json")));
        assertTrue(r.backups.isEmpty());
    }

    @Test
    void aCorruptOldFileGivesDefaultsAndIsLogged() throws IOException {
        put("book.json", "{ \"creative_edits\": false, ");
        put("client.json", "not json at all");
        ConfigMigration.Result r = ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertEquals(defaults(ConfigKeys.Side.COMMON), common());
        assertEquals(defaults(ConfigKeys.Side.CLIENT), client());
        assertEquals(2, r.log.stream().filter(l -> l.contains("invalid")).count(), r.log.toString());
        assertEquals("{ \"creative_edits\": false, ", Files.readString(dir.resolve("barelyquesting/book.json")));
        assertEquals("not json at all", Files.readString(dir.resolve("barelyquesting/client.json")));
        assertTrue(r.backups.isEmpty());
        assertFalse(Files.exists(dir.resolve("barelyquesting/book.json.back")) || Files.exists(dir.resolve("barelyquesting/client.json.back")));
    }

    @Test
    void aPartiallyFilledOldFileKeepsWhatItHas() throws IOException {
        put("client.json", "{\"hud\": false}");
        put("book.json", "{\"title\": \"Pack\", \"chapters\": [\"a\"], \"editors\": [\"zed\"]}");
        ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertTrue(Files.readString(dir.resolve("barelyquesting/book.json")).contains("\"chapters\""), "the book data stays in book.json");
        assertFalse(Files.readString(dir.resolve("barelyquesting/book.json")).contains("editors"));
        Map<String, Object> k = client();
        assertEquals(false, k.get("hud_tracker"));
        assertEquals("graph", k.get("view"));
        assertEquals("", k.get("theme"));
        Map<String, Object> c = common();
        assertEquals(List.of("zed"), c.get("editors"));
        assertEquals(true, c.get("creative_edits"));
    }

    @Test
    void invalidValuesFallBackToTheirDefaultAndAreLogged() throws IOException {
        put("client.json", "{\"hud\": [1], \"view\": \"sideways\", \"theme\": \"barelyquesting:parchment\", \"pinned\": [\"a\", {}, \"b\"], \"foo\": 1}");
        put("book.json", "{\"editing\": {}, \"editors\": \"bob\", \"creative_edits\": \"false\", \"title\": \"Pack Book\", \"chapters\": []}");
        ConfigMigration.Result r = ConfigMigration.run(dir, "barelyquesting-common.toml");
        Map<String, Object> k = client();
        assertEquals(true, k.get("hud_tracker"));
        assertEquals("graph", k.get("view"));
        assertEquals("barelyquesting:parchment", k.get("theme"));
        assertEquals(List.of("a", "b"), k.get("pinned_quests"));
        Map<String, Object> c = common();
        assertEquals(true, c.get("editing"));
        assertEquals(List.of(), c.get("editors"));
        assertEquals(false, c.get("creative_edits"), "the old reader took the string false as false");
        String log = String.join("\n", r.log);
        assertTrue(log.contains("invalid hud"), log);
        assertTrue(log.contains("invalid view"), log);
        assertTrue(log.contains("invalid editing"), log);
        assertTrue(log.contains("invalid editors"), log);
        assertTrue(log.contains("ignored unknown key foo"), log);
        assertFalse(log.contains("title"), "book data keys are not unknown keys: " + log);
        // partly readable: both originals stay exactly where they are and no backup is made
        assertTrue(Files.readString(dir.resolve("barelyquesting/client.json")).contains("sideways"));
        assertTrue(Files.readString(dir.resolve("barelyquesting/book.json")).contains("\"editors\": \"bob\""));
        assertTrue(r.backups.isEmpty());
    }

    @Test
    void numbersAndOnOffStringsMeanWhatTheOldReaderMeant() throws IOException {
        put("client.json", "{\"hud\": 0}");
        put("book.json", "{\"creative_edits\": 0, \"give_book_on_join\": 1, \"editing\": \"OFF\"}");
        ConfigMigration.run(dir, "barelyquesting-common.toml");
        assertEquals(false, client().get("hud_tracker"));
        Map<String, Object> c = common();
        assertEquals(false, c.get("creative_edits"));
        assertEquals(true, c.get("give_book_on_join"));
        assertEquals(false, c.get("editing"));
    }

    @Test
    void theNeoForge26_3StartupFileNameIsUsed() throws IOException {
        put("book.json", "{\"creative_edits\": false}");
        ConfigMigration.Result r = ConfigMigration.run(dir, "barelyquesting-startup.toml");
        assertEquals(1, r.written.size());
        assertEquals(false, readToml(dir.resolve("barelyquesting-startup.toml")).get("creative_edits"));
    }

    // ---- the toml itself ----

    @Test
    void theTomlCarriesTheCommentsAndRoundTripsAwkwardStrings() throws IOException {
        Map<String, Object> values = new LinkedHashMap<>(defaults(ConfigKeys.Side.CLIENT));
        values.put("theme", "a \"quoted\" \\ theme\twith tab");
        values.put("pinned_quests", List.of("x", "y\"z"));
        String text = ConfigMigration.toml(ConfigKeys.Side.CLIENT, values);
        assertTrue(text.contains("#Id of the book theme"), text);
        assertFalse(text.contains("\n\n"), "no blank lines: the loader writes none");
        Path f = dir.resolve("t.toml");
        Files.writeString(f, text);
        Map<String, Object> back = readToml(f);
        assertEquals(values.get("theme"), back.get("theme"));
        assertEquals(values.get("pinned_quests"), back.get("pinned_quests"));
    }

    @Test
    void settingsTakeLoadedValuesAndWriteChangesBack() {
        List<Map<String, Object>> written = new ArrayList<>();
        Settings.writer(written::add);
        try {
            Settings.load(Map.of("editing", false, "editors", List.of("Bob"), "view", "nonsense", "hud_tracker", "not a boolean"));
            assertFalse(Settings.editing());
            assertTrue(Settings.isEditor("bob"));
            assertEquals("graph", Settings.str("view"), "a value outside the allowed list is ignored");
            assertTrue(Settings.bool("hud_tracker"), "a value of the wrong type is ignored");
            Settings.set("theme", "barelyquesting:parchment");
            assertEquals("barelyquesting:parchment", Settings.str("theme"));
            assertEquals(1, written.size());
            assertEquals("barelyquesting:parchment", written.get(0).get("theme"));
        } finally {
            Settings.writer(null);
        }
    }

    // ---- helpers ----

    private static Map<String, Object> defaults(ConfigKeys.Side side) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (ConfigKeys.Key k : ConfigKeys.keys(side)) {
            out.put(k.name(), k.def());
        }
        return out;
    }

    /** A reader for the small subset of TOML the migration writes: key = true | "string" | ["a", "b"]. */
    static Map<String, Object> readToml(Path file) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            int eq = t.indexOf(" = ");
            String key = t.substring(0, eq);
            String v = t.substring(eq + 3).trim();
            if (v.equals("true") || v.equals("false")) {
                out.put(key, Boolean.valueOf(v));
            } else if (v.startsWith("[")) {
                List<String> items = new ArrayList<>();
                int i = 1;
                while (i < v.length()) {
                    if (v.charAt(i) == '"') {
                        StringBuilder sb = new StringBuilder();
                        i = unquote(v, i, sb);
                        items.add(sb.toString());
                    } else {
                        i++;
                    }
                }
                out.put(key, items);
            } else {
                StringBuilder sb = new StringBuilder();
                unquote(v, 0, sb);
                out.put(key, sb.toString());
            }
        }
        return out;
    }

    /** Reads the quoted string starting at index i; returns the index after its closing quote. */
    private static int unquote(String v, int i, StringBuilder sb) {
        i++;
        while (v.charAt(i) != '"') {
            char c = v.charAt(i);
            if (c == '\\') {
                i++;
                char n = v.charAt(i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(v.substring(i + 1, i + 5), 16));
                        i += 4;
                        break;
                    default: sb.append(n);
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        return i + 1;
    }
}

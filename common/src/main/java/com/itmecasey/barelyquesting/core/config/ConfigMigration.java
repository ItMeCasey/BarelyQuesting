package com.itmecasey.barelyquesting.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.itmecasey.barelyquesting.core.Json;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Carries the settings of released versions over to the loader's .toml files. Before 2026.11.9 the settings lived in two JSON files:
 * {@code config/barelyquesting/book.json} (editing, creative_edits, editors, give_book_on_join, open_book_on_join next to the quest
 * data) and {@code config/barelyquesting/client.json} (view, chapter, hud, theme, pinned).
 *
 * <p>The import only runs when the new toml does not exist yet and the old file does. In order: write the toml, read it back and check
 * every value, check that the old file parsed cleanly, write {@code <old>.back} (a header line plus the exact original bytes), check the
 * backup, and only then take the settings out of the old file: client.json is removed, and book.json (which also holds the quest book's
 * title and chapter order) is rewritten without the setting keys. If any step fails, or the old file is corrupt or only partly readable,
 * the original stays exactly as it is, the reason is logged, and unreadable values use their defaults. A backup is never overwritten.
 */
public final class ConfigMigration {
    private static final Logger LOG = Logger.getLogger("barelyquesting");

    /** What a run did: the files it wrote and one line per migrated, ignored or invalid value. */
    public static final class Result {
        public final List<Path> written = new ArrayList<>();
        public final List<Path> backups = new ArrayList<>();
        public final List<String> log = new ArrayList<>();
    }

    private ConfigMigration() {
    }

    /** Old file key to new key, per old file. */
    private static final Map<String, String> BOOK_KEYS = new LinkedHashMap<>();
    private static final Map<String, String> CLIENT_KEYS = new LinkedHashMap<>();

    static {
        BOOK_KEYS.put("editing", "editing");
        BOOK_KEYS.put("creative_edits", "creative_edits");
        BOOK_KEYS.put("editors", "editors");
        BOOK_KEYS.put("give_book_on_join", "give_book_on_join");
        BOOK_KEYS.put("open_book_on_join", "open_book_on_join");
        CLIENT_KEYS.put("hud", "hud_tracker");
        CLIENT_KEYS.put("theme", "theme");
        CLIENT_KEYS.put("view", "view");
        CLIENT_KEYS.put("chapter", "last_chapter");
        CLIENT_KEYS.put("pinned", "pinned_quests");
    }

    /**
     * Runs the migration for both sides. {@code commonFile} is the name of the common toml in the config folder (the loader's name for it,
     * for example {@code barelyquesting-common.toml}); the client toml is {@code barelyquesting-client.toml}.
     */
    public static Result run(Path configDir, String commonFile) {
        Result r = new Result();
        String version = modVersion();
        Path dir = configDir.resolve("barelyquesting");
        migrate(r, version, dir.resolve("book.json"), configDir.resolve(commonFile), "config/" + commonFile, ConfigKeys.Side.COMMON, BOOK_KEYS, true);
        migrate(r, version, dir.resolve("client.json"), configDir.resolve("barelyquesting-client.toml"), "config/barelyquesting-client.toml",
                ConfigKeys.Side.CLIENT, CLIENT_KEYS, false);
        return r;
    }

    /** What reading one old file gave. */
    private static final class Imported {
        Map<String, Object> values;
        JsonObject object; // the parsed file, or null when it could not be parsed
        boolean clean;
    }

    private static void migrate(Result r, String version, Path old, Path toml, String tomlName, ConfigKeys.Side side, Map<String, String> keys,
            boolean mixedWithData) {
        if (Files.exists(toml) || !Files.isRegularFile(old)) {
            return;
        }
        List<String> log = new ArrayList<>();
        String name = old.getFileName().toString();
        byte[] original;
        try {
            original = Files.readAllBytes(old);
        } catch (IOException | RuntimeException e) {
            original = null;
            log.add(name + ": could not be read (" + e.getMessage() + "); every value uses its default, the file stays where it is");
        }
        Imported in = new Imported();
        if (original == null) {
            in.values = defaults(side);
        } else {
            in = importValues(name, new String(original, StandardCharsets.UTF_8), side, keys, log);
            if (mixedWithData && in.object != null && keys.keySet().stream().noneMatch(in.object::has)) {
                return; // book.json without any setting keys: nothing to migrate
            }
        }
        boolean done = false;
        Path backup = null;
        try {
            Files.createDirectories(toml.getParent());
            Files.writeString(toml, toml(side, in.values), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            r.written.add(toml);
            Map<String, Object> back = parseToml(Files.readString(toml, StandardCharsets.UTF_8));
            for (ConfigKeys.Key k : ConfigKeys.keys(side)) {
                if (!sameValue(in.values.get(k.name()), back.get(k.name()))) {
                    throw new IOException("the toml does not hold " + k.name() + " as written");
                }
            }
            if (!in.clean || original == null) {
                log.add(name + ": left in place because it was not fully readable; " + toml.getFileName() + " has the readable values and defaults for the rest");
            } else {
                backup = backupName(old);
                String header = "Migrated by Barely Questing " + version + " on " + LocalDate.now() + " to " + tomlName
                        + "; this file is no longer read. Kept as a backup.\n";
                byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
                byte[] all = new byte[headerBytes.length + original.length];
                System.arraycopy(headerBytes, 0, all, 0, headerBytes.length);
                System.arraycopy(original, 0, all, headerBytes.length, original.length);
                Files.write(backup, all, StandardOpenOption.CREATE_NEW);
                byte[] check = Files.readAllBytes(backup);
                if (!Arrays.equals(check, all)) {
                    throw new IOException("the backup " + backup.getFileName() + " does not match the original");
                }
                r.backups.add(backup);
                if (mixedWithData) {
                    JsonObject stripped = in.object.deepCopy();
                    keys.keySet().forEach(stripped::remove);
                    Json.writeAtomic(old, Json.pretty(stripped));
                } else {
                    Files.delete(old);
                }
                done = true;
                log.add("migrated " + old + " -> " + toml + "; the original is kept as " + backup);
            }
        } catch (FileAlreadyExistsException e) {
            if (backup == null) {
                return; // the loader or the user got there first: never overwrite
            }
            log.add(name + ": a file appeared while migrating (" + e.getFile() + "); the original stays where it is");
        } catch (IOException | RuntimeException e) {
            log.add(name + ": could not migrate (" + e.getMessage() + "); the original stays where it is");
        }
        for (String line : log) {
            boolean problem = line.contains("invalid") || line.contains("could not") || line.contains("left in place") || line.contains("appeared");
            LOG.log(problem ? Level.WARNING : Level.INFO, "Barely Questing config migration: " + line);
        }
        r.log.addAll(log);
        if (!done && !r.written.contains(toml) && Files.exists(toml)) {
            r.written.add(toml);
        }
    }

    /** {@code old + ".back"}, or ".back2", ".back3" and so on when that name is taken; an existing backup is never overwritten. */
    private static Path backupName(Path old) {
        Path p = old.resolveSibling(old.getFileName() + ".back");
        for (int i = 2; Files.exists(p); i++) {
            p = old.resolveSibling(old.getFileName() + ".back" + i);
        }
        return p;
    }

    private static boolean sameValue(Object expected, Object actual) {
        if (expected instanceof List && actual instanceof List) {
            return expected.toString().equals(actual.toString());
        }
        return expected == null ? actual == null : expected.equals(actual);
    }

    private static Map<String, Object> defaults(ConfigKeys.Side side) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (ConfigKeys.Key k : ConfigKeys.keys(side)) {
            out.put(k.name(), k.def());
        }
        return out;
    }

    /** Reads the old file's text into new key values. A corrupt or empty file gives all defaults; a bad value gives its default. */
    private static Imported importValues(String fileName, String text, ConfigKeys.Side side, Map<String, String> keys, List<String> log) {
        Imported out = new Imported();
        out.values = defaults(side);
        if (text.trim().isEmpty()) {
            log.add(fileName + ": empty, nothing to migrate; every value uses its default");
            return out;
        }
        JsonObject o;
        try {
            JsonElement e = Json.parse(text);
            if (e == null || !e.isJsonObject()) {
                log.add(fileName + ": invalid, not a JSON object; every value uses its default");
                return out;
            }
            o = e.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException ex) {
            log.add(fileName + ": invalid JSON (" + ex.getMessage() + "); every value uses its default");
            return out;
        }
        out.object = o;
        out.clean = true;
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (!keys.containsKey(e.getKey()) && !isDataKey(fileName, e.getKey())) {
                log.add(fileName + ": ignored unknown key " + e.getKey());
            }
        }
        for (Map.Entry<String, String> m : keys.entrySet()) {
            JsonElement v = o.get(m.getKey());
            if (v == null) {
                continue;
            }
            ConfigKeys.Key key = ConfigKeys.key(m.getValue());
            int before = log.size();
            Object converted = convert(fileName, m.getKey(), key, v, log);
            if (log.size() > before) {
                out.clean = false; // a value could not be read as it is
            }
            if (converted != null) {
                out.values.put(key.name(), converted);
                log.add(fileName + ": " + m.getKey() + " -> " + key.name() + " = " + converted);
            }
        }
        return out;
    }

    /** book.json also holds quest data, which is not a setting and never counts as an unknown key. */
    private static boolean isDataKey(String fileName, String key) {
        return fileName.equals("book.json") && (key.equals("format") || key.equals("title") || key.equals("chapters"));
    }

    /** The new value for an old JSON value, or null (with a log line) when the old value is not usable. */
    private static Object convert(String fileName, String oldKey, ConfigKeys.Key key, JsonElement v, List<String> log) {
        String bad = fileName + ": invalid " + oldKey + " (" + v + "); using the default " + key.def();
        switch (key.type()) {
            case BOOL: {
                if (!v.isJsonPrimitive()) {
                    log.add(bad);
                    return null;
                }
                JsonPrimitive p = v.getAsJsonPrimitive();
                if (p.isBoolean()) {
                    return p.getAsBoolean();
                }
                if (p.isNumber()) {
                    return p.getAsInt() != 0; // the old reader took 0 and 1 as booleans
                }
                String s = p.getAsString().trim().toLowerCase(Locale.ROOT);
                if (key.name().equals("editing")) {
                    if (s.equals("off")) {
                        return false;
                    }
                    if (s.equals("on")) {
                        return true;
                    }
                }
                if (s.equals("true")) {
                    return true;
                }
                if (s.equals("false")) {
                    return false;
                }
                log.add(bad);
                return null;
            }
            case STRING: {
                if (!v.isJsonPrimitive()) {
                    log.add(bad);
                    return null;
                }
                return v.getAsString();
            }
            case CHOICE: {
                String s = v.isJsonPrimitive() ? v.getAsString().trim().toLowerCase(Locale.ROOT) : "";
                if (key.choices().contains(s)) {
                    return s;
                }
                log.add(bad);
                return null;
            }
            case LIST: {
                if (!v.isJsonArray()) {
                    log.add(bad);
                    return null;
                }
                List<String> out = new ArrayList<>();
                JsonArray arr = v.getAsJsonArray();
                for (JsonElement e : arr) {
                    if (e.isJsonPrimitive()) {
                        out.add(e.getAsString());
                    } else {
                        log.add(fileName + ": invalid entry in " + oldKey + " (" + e + "); skipped");
                    }
                }
                return out;
            }
            default:
                return null;
        }
    }

    // ---- the mod version for the backup header ----

    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"");

    /** The mod's version from its mods.toml (the same file on every loader), or "unknown". */
    static String modVersion() {
        for (String file : new String[] {"META-INF/neoforge.mods.toml", "META-INF/mods.toml"}) {
            try (InputStream in = ConfigMigration.class.getClassLoader().getResourceAsStream(file)) {
                if (in != null) {
                    Matcher m = VERSION.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            } catch (IOException | RuntimeException e) {
                // fall through to the next file
            }
        }
        return "unknown";
    }

    // ---- TOML ----

    /**
     * The TOML text for one side with the given values (missing keys get their default). The layout is exactly what the loaders write
     * ({@code #comment} line, then {@code key = value}, no blank lines) so the loader accepts the file as it is and does not rewrite it.
     */
    public static String toml(ConfigKeys.Side side, Map<String, Object> values) {
        StringBuilder sb = new StringBuilder();
        for (ConfigKeys.Key k : ConfigKeys.keys(side)) {
            sb.append('#').append(k.comment()).append('\n');
            Object v = values.containsKey(k.name()) ? values.get(k.name()) : k.def();
            sb.append(k.name()).append(" = ").append(value(v)).append('\n');
        }
        return sb.toString();
    }

    /** Reads the small subset of TOML {@link #toml} writes: {@code key = true}, {@code key = "text"}, {@code key = ["a", "b"]}. */
    public static Map<String, Object> parseToml(String text) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            int eq = t.indexOf(" = ");
            if (eq < 0) {
                continue;
            }
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
            } else if (v.startsWith("\"")) {
                StringBuilder sb = new StringBuilder();
                unquote(v, 0, sb);
                out.put(key, sb.toString());
            }
        }
        return out;
    }

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

    private static String value(Object v) {
        if (v instanceof Boolean) {
            return v.toString();
        }
        if (v instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : (List<?>) v) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(quote(String.valueOf(o)));
            }
            return sb.append(']').toString();
        }
        return quote(String.valueOf(v));
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}

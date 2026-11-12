package com.itmecasey.barelyquesting.core.store;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Chapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads and writes the quest definitions as readable JSON files: {@code book.json} (title, settings, chapter order) and one
 * {@code chapters/<id>.json} per chapter. Hand edits are fine; files that are not in the chapter order are appended at the end.
 */
public final class QuestStore {
    private final Path dir;

    public QuestStore(Path dir) {
        this.dir = dir;
    }

    public Path dir() {
        return dir;
    }

    public Path chapterFile(String id) {
        return dir.resolve("chapters").resolve(id + ".json");
    }

    public boolean exists() {
        return Files.exists(dir.resolve("book.json"));
    }

    public Book load() throws IOException {
        Book book = new Book();
        Path header = dir.resolve("book.json");
        List<String> order = new ArrayList<>();
        if (Files.exists(header)) {
            JsonObject o = Json.parseObject(Files.readString(header, StandardCharsets.UTF_8));
            book.title = Json.str(o, "title", book.title);
            book.creativeEdits = Json.bool(o, "creative_edits", true);
            book.readSettings(o);
            for (JsonElement e : Json.arr(o, "editors")) {
                book.editors.add(e.getAsString().toLowerCase(java.util.Locale.ROOT));
            }
            for (JsonElement e : Json.arr(o, "chapters")) {
                order.add(e.getAsString());
            }
        }
        Path chapters = dir.resolve("chapters");
        Set<String> seen = new HashSet<>();
        if (Files.isDirectory(chapters)) {
            List<String> onDisk = new ArrayList<>();
            try (Stream<Path> files = Files.list(chapters)) {
                files.forEach(p -> {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".json")) {
                        onDisk.add(name.substring(0, name.length() - 5));
                    }
                });
            }
            java.util.Collections.sort(onDisk);
            List<String> all = new ArrayList<>(order);
            for (String id : onDisk) {
                if (!all.contains(id)) {
                    all.add(id);
                }
            }
            for (String id : all) {
                Path f = chapterFile(id);
                if (!Files.exists(f) || !seen.add(id)) {
                    continue;
                }
                Chapter c = Chapter.fromJson(Json.parseObject(Files.readString(f, StandardCharsets.UTF_8)));
                if (c.id.isEmpty()) {
                    c.id = id;
                }
                book.chapters.add(c);
            }
        }
        book.repair();
        return book;
    }

    public void save(Book book) throws IOException {
        Json.writeAtomic(dir.resolve("book.json"), Json.pretty(book.headerJson()));
        Set<String> keep = new HashSet<>();
        for (Chapter c : book.chapters) {
            keep.add(c.id + ".json");
            Json.writeAtomic(chapterFile(c.id), Json.pretty(c.toJson()));
        }
        Path chapters = dir.resolve("chapters");
        if (Files.isDirectory(chapters)) {
            try (Stream<Path> files = Files.list(chapters)) {
                for (Path p : (Iterable<Path>) files::iterator) {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".json") && !keep.contains(name)) {
                        Files.deleteIfExists(p);
                    }
                }
            }
        }
    }
}

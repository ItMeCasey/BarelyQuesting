package com.itmecasey.barelyquesting.core.imports;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.net.QuestServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts other quest mods' books into ours, once. There is no runtime dependency on those mods: the files are read as plain data and
 * the result goes in as one batch of edit operations, so it is saved, synced to everyone, and undone with one Undo.
 */
public final class Importer {
    /** What an import did. {@code error} is null on success. */
    public static final class Result {
        public String error;
        public String format = "";
        public int chapters;
        public int quests;
        public List<String> skipped = new ArrayList<>();
    }

    private Importer() {
    }

    /** Detects the format of a file or folder, converts it and adds the chapters to the server's book. */
    public static Result importPath(QuestServer server, Path path) {
        Result r = new Result();
        if (!Files.exists(path)) {
            r.error = "path does not exist: " + path;
            return r;
        }
        Book book = server.engine().book();
        List<Chapter> chapters;
        try {
            if (BqImporter.looks(path)) {
                BqImporter bq = new BqImporter(book);
                bq.load(path);
                chapters = bq.chapters;
                r.format = "Better Questing";
                r.skipped = bq.skipped;
            } else if (FtbImporter.looks(path)) {
                FtbImporter ftb = new FtbImporter(book);
                ftb.load(path);
                chapters = ftb.chapters;
                r.format = "FTB Quests";
                r.skipped = ftb.skipped;
            } else {
                r.error = "not a Better Questing or FTB Quests folder or file";
                return r;
            }
        } catch (java.io.IOException | RuntimeException e) {
            r.error = String.valueOf(e.getMessage());
            return r;
        }
        return apply(server, chapters, r);
    }

    /** Adds converted chapters to the book as a single undoable batch. */
    public static Result apply(QuestServer server, List<Chapter> chapters, Result r) {
        List<JsonObject> ops = new ArrayList<>();
        for (Chapter c : chapters) {
            ops.add(EditOps.putChapter(c, null));
        }
        // Quests go in after all chapters exist; dependencies across chapters need every quest present, so add in two passes.
        List<JsonObject> deps = new ArrayList<>();
        for (Chapter c : chapters) {
            for (com.itmecasey.barelyquesting.core.model.Quest q : c.quests) {
                com.itmecasey.barelyquesting.core.model.Quest bare = q.copy();
                bare.dependencies.clear();
                ops.add(EditOps.putQuest(c.id, bare, null));
                if (!q.dependencies.isEmpty()) {
                    deps.add(EditOps.putQuest(c.id, q, null));
                }
                r.quests++;
            }
            r.chapters++;
        }
        ops.addAll(deps);
        if (!ops.isEmpty() && !server.applyEdit(null, EditOps.batch(ops), null)) {
            r.error = "the book rejected the imported data";
        }
        return r;
    }
}

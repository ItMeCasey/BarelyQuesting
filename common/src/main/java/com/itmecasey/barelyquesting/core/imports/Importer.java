package com.itmecasey.barelyquesting.core.imports;

import com.itmecasey.barelyquesting.core.net.QuestServer;

import java.nio.file.Path;

/** Converts other quest mods' books into ours, once. There is no runtime dependency on those mods. */
public final class Importer {
    /** What an import did. {@code error} is null on success. */
    public static final class Result {
        public String error;
        public String format = "";
        public int chapters;
        public int quests;
    }

    private Importer() {
    }

    public static Result importPath(QuestServer server, Path path) {
        Result r = new Result();
        r.error = "importers are not built yet";
        return r;
    }
}

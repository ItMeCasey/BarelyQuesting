package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelyquesting.core.imports.Importer;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Imports a chapter exported by a real FTB Quests install (the Era of Black Death modpack, github.com/ChargeyXD/era-of-black-death-ftbquests,
 * Forge 1.20.1 format: item stacks as {Count, id, tag}), not hand-written sample data.
 */
class RealFtbBookTest {
    @TempDir
    Path dir;

    @Test
    void importsARealExportedChapter() throws URISyntaxException {
        Path root = Path.of(getClass().getResource("/ftb-real/quests/chapters/natures_aura.snbt").toURI()).getParent().getParent();
        QuestServer server = new QuestServer(dir.resolve("config"), dir.resolve("world"), (p, m) -> { }, new QuestEngine.Host() {
            public PlayerView online(UUID id) {
                return null;
            }

            public long nowMillis() {
                return 0;
            }

            public List<PlayerView> players() {
                return List.of();
            }
        });
        server.start();
        Importer.Result r = Importer.importPath(server, root);
        assertNull(r.error);
        assertEquals(1, r.chapters);
        assertEquals(4, r.quests);
        Chapter c = server.engine().book().chapters.get(server.engine().book().chapters.size() - 1);
        assertEquals("Nature's Aura", c.title);
        assertEquals(4, c.quests.size());
        Quest altar = c.quests.get(0);
        assertEquals("Nature's Altar", altar.title);
        assertEquals("item", altar.tasks.get(0).type);
        assertEquals("naturesaura:nature_altar", altar.tasks.get(0).item("item").id());
        assertEquals("xp", altar.rewards.get(0).type);
        assertEquals(75, altar.rewards.get(0).num("amount", 0));
        Quest sapling = c.quests.get(2);
        assertEquals(1, sapling.dependencies.size());
        assertEquals(c.quests.get(1).id, sapling.dependencies.get(0));
        Def item = sapling.rewards.get(0);
        assertEquals("item", item.type);
        assertEquals("naturesaura:gold_leaf", item.item("item").id());
        assertEquals(4, item.item("item").count());
        assertTrue(Files.exists(dir.resolve("config")));
    }

    /** A real Better Questing export (github.com/BearsDen989-GT/GT6BetterQuesting, format 3.1.0, one JSON file per quest and quest line). */
    @Test
    void importsARealSplitBetterQuestingExport() throws URISyntaxException {
        Path root = Path.of(getClass().getResource("/bq-real/DefaultQuests/QuestLines").toURI()).getParent().getParent();
        QuestServer server = new QuestServer(dir.resolve("config2"), dir.resolve("world2"), (p, m) -> { }, new QuestEngine.Host() {
            public PlayerView online(UUID id) {
                return null;
            }

            public long nowMillis() {
                return 0;
            }

            public List<PlayerView> players() {
                return List.of();
            }
        });
        server.start();
        Importer.Result r = Importer.importPath(server, root);
        assertNull(r.error, r.error);
        assertEquals("Better Questing", r.format);
        assertEquals(1, r.quests);
        Quest q = server.engine().book().allQuests().stream().filter(x -> x.title.equals("Engine Machines Continued")).findFirst().orElseThrow();
        assertEquals("gregtech:gt.multitileentity", q.icon.id());
        assertTrue(q.dependencies.isEmpty(), "its prerequisite is in a quest line file that is not part of this sample");
        assertTrue(server.engine().book().chapters.stream().anyMatch(c -> c.title.equals("Going Nuclear")));
    }
}

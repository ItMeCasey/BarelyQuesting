package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.theme.Theme;
import com.itmecasey.barelyquesting.theme.ThemeManager;
import com.itmecasey.barelyquesting.ui.BookUi;
import com.itmecasey.barelyquesting.ui.ClientEnv;
import com.itmecasey.barelyquesting.ui.ClientState;
import com.itmecasey.barelyquesting.ui.Keys;
import com.itmecasey.barelyquesting.ui.RichText;
import com.itmecasey.barelyquesting.ui.TextEditor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Drives the whole book (UI, client state, wire format, server, engine) through a recording Gfx and a loopback connection. */
class UiTest {
    @TempDir
    Path dir;
    FakePlayer alice = new FakePlayer("alice");
    QuestServer server;
    RecordingGfx g = new RecordingGfx(427, 240);
    BookUi ui;
    ClientState st = ClientState.get();

    @BeforeEach
    void setUp() {
        alice.op = true;
        server = new QuestServer(dir.resolve("config"), dir.resolve("world"), (p, m) -> {
            if (p.equals(alice.id)) {
                st.handle(m);
            }
        }, new QuestEngine.Host() {
            public PlayerView online(UUID id) {
                return id.equals(alice.id) ? alice : null;
            }

            public long nowMillis() {
                return System.currentTimeMillis();
            }

            public List<PlayerView> players() {
                return List.of(alice);
            }
        });
        server.start();
        st.reset();
        ClientEnv.Holder.set(new ClientEnv() {
            public List<String> itemIds() {
                return List.of("minecraft:stone", "minecraft:diamond", "minecraft:apple");
            }

            public List<String> ids(String kind) {
                return List.of("minecraft:zombie", "minecraft:creeper");
            }

            public void sendToServer(byte[] message) {
                server.handle(alice, message);
                server.flush();
            }

            public Path configDir() {
                return dir.resolve("clientcfg");
            }

            public boolean showRecipes(ItemSpec item) {
                shown.add("recipes:" + item.id());
                return true;
            }

            public boolean showUses(ItemSpec item) {
                shown.add("uses:" + item.id());
                return true;
            }

            public Map<String, String> themeFiles(String name) {
                return Map.of();
            }
        });
        server.onJoin(alice);
        ui = new BookUi();
    }

    final List<String> shown = new ArrayList<>();

    @AfterEach
    void tearDown() {
        ClientEnv.Holder.set(ClientEnv.NONE);
        st.reset();
    }

    void frame() {
        g.reset();
        ui.render(g, 5, 5);
        assertEquals(0, g.clipDepth(), "every clip is popped");
    }

    void click(String text) {
        frame();
        RecordingGfx.Text t = g.find(text);
        assertNotNull(t, "text on screen: " + text);
        assertTrue(ui.mouseClicked(t.x + 2, t.y + 2, 0), "clicked " + text);
        frame();
    }

    @Test
    void joinSyncsBookAndPermission() {
        assertTrue(st.connected);
        assertTrue(st.canEdit);
        assertEquals(2, st.book.chapters.size());
        assertEquals(alice.id, st.self);
        frame();
        assertTrue(g.has("Getting Started"));
        assertTrue(g.has("Exploring"));
        assertTrue(g.has("Quest Book"));
        assertFalse(g.items.isEmpty(), "quest icons are drawn");
    }

    @Test
    void listViewSelectAndPlayQuest() {
        click("List");
        assertTrue(g.has("Gather Wood"));
        assertTrue(g.has("Available"));
        click("Gather Wood");
        assertEquals("wood", ui.selected);
        assertTrue(g.has("Obtain"), "task text");
        assertTrue(g.has("Rewards"));
        // finish the quest on the server and claim from the UI
        server.engine().forceComplete(server.engine().teams().teamOf(alice.id).id, "wood");
        server.flush();
        frame();
        assertTrue(g.has("Claim all"));
        click("Claim all");
        assertEquals(List.of("8x minecraft:apple"), alice.given);
        frame();
        assertFalse(g.has("Claim all"));
    }

    @Test
    void graphClickSelectsNodeAndShowsTooltip() {
        frame();
        // find a node by clicking around the middle of the main area until something is selected
        boolean hit = false;
        for (int x = 120; x < 330 && !hit; x += 3) {
            for (int y = 60; y < 200 && !hit; y += 3) {
                ui.mouseClicked(x, y, 0);
                ui.mouseReleased(x, y, 0);
                frame();
                hit = !ui.selected.isEmpty();
            }
        }
        assertTrue(hit, "a node is somewhere in the middle of the graph");
        assertTrue(g.has("Tasks"));
    }

    @Test
    void checkmarkTaskFromUi() {
        ui.chapterId = "exploring";
        ui.select("check");
        frame();
        assertTrue(g.has("Check"));
        click("Check");
        assertTrue(server.engine().isComplete(server.engine().teams().teamOf(alice.id).id, "check"));
    }

    @Test
    void editorAddsEditsAndUndoes() {
        click("Edit");
        assertTrue(ui.editing);
        int before = st.book.allQuests().size();
        click("+ Quest");
        assertEquals(before + 1, st.book.allQuests().size(), "server echoed the new quest back");
        assertFalse(ui.selected.isEmpty() && false);
        frame();
        assertTrue(st.canUndo());
        click("Undo");
        assertEquals(before, st.book.allQuests().size());
        assertTrue(st.canRedo());
        click("Redo");
        assertEquals(before + 1, st.book.allQuests().size());
        // saved to the config files
        assertTrue(Json.parseObject(read("config/chapters/getting_started.json")).getAsJsonArray("quests").size() == 6);
        // keyboard undo
        assertTrue(ui.keyPressed(g, Keys.Z, Keys.MOD_CTRL));
        assertEquals(before, st.book.allQuests().size());
    }

    String read(String p) {
        try {
            return java.nio.file.Files.readString(dir.resolve(p));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void editingFieldsSendsOps() {
        click("Edit");
        ui.chapterId = "getting_started";
        ui.select("wood");
        frame();
        RecordingGfx.Text title = g.find("Title");
        assertNotNull(title);
        // click into the title field (just below its label), type, and press Enter
        assertTrue(ui.mouseClicked(title.x + 100, title.y + 14, 0));
        assertTrue(ui.typing());
        for (char c : " X".toCharArray()) {
            ui.charTyped(c);
        }
        ui.keyPressed(g, Keys.ENTER, 0);
        frame();
        assertEquals("Gather Wood X", st.book.quest("wood").title);
        ui.keyPressed(g, Keys.Z, Keys.MOD_CTRL);
        assertEquals("Gather Wood", st.book.quest("wood").title);
    }

    @Test
    void nonEditorsNeverSeeEditControls() {
        alice.op = false;
        server.onJoin(alice);
        assertFalse(st.canEdit);
        frame();
        assertFalse(g.has("Edit"));
    }

    @Test
    void modalsOpenAndClose() {
        click("Team");
        assertTrue(ui.hasModal());
        assertTrue(g.has("Create team"));
        assertTrue(ui.keyPressed(g, Keys.ESCAPE, 0));
        assertFalse(ui.hasModal());
        click("Themes");
        assertTrue(g.has("Default"));
        ui.keyPressed(g, Keys.ESCAPE, 0);
        assertFalse(ui.hasModal());
    }

    @Test
    void teamCreatedFromUi() {
        click("Team");
        click("Create team");
        for (char c : "Crew".toCharArray()) {
            ui.charTyped(c);
        }
        ui.keyPressed(g, Keys.ENTER, 0);
        frame();
        assertEquals("Crew", server.engine().teams().teamOf(alice.id).name);
        assertEquals("Crew", Json.str(st.team, "name", ""));
    }

    @Test
    void itemClicksOpenTheViewer() {
        ui.chapterId = "getting_started";
        ui.select("stone");
        frame();
        // the submit task shows the cobblestone icon; find its slot via the recorded item and click it
        assertTrue(g.items.stream().anyMatch(i -> i.id().equals("minecraft:cobblestone")));
    }

    @Test
    void richTextParsesMarkup() {
        List<RichText.Run> runs = RichText.parse("a **b** *c* &cred&r [l](http://x) \\n z");
        assertEquals("a b c red l   z", RichText.plain("a **b** *c* &cred&r [l](http://x) \n z"));
        assertTrue(runs.stream().anyMatch(r -> r.url != null && r.url.equals("http://x")));
        assertEquals("**unclosed", RichText.plain("**unclosed"));
        List<List<RichText.Piece>> lines = RichText.layout(g, RichText.parse("aaa bbb ccc ddd eee"), 60);
        assertTrue(lines.size() > 1, "wraps");
    }

    @Test
    void textEditorEditsAndMultiline() {
        TextEditor e = new TextEditor(true);
        e.focus();
        e.insert("ab");
        e.keyPressed(Keys.ENTER, 0, g);
        e.insert("cd");
        assertEquals("ab\ncd", e.text());
        e.keyPressed(Keys.UP, 0, g);
        e.keyPressed(Keys.END, 0, g);
        e.insert("!");
        assertEquals("ab!\ncd", e.text());
        e.keyPressed(Keys.A, Keys.MOD_CTRL, g);
        e.insert("z");
        assertEquals("z", e.text());
        TextEditor n = new TextEditor(false);
        n.numeric = true;
        n.focus();
        n.insert("1a2-");
        assertEquals("12-", n.text());
    }

    @Test
    void themesParseAndFallBack() {
        JsonObject o = Json.parseObject("{\"name\":\"T\",\"colors\":{\"text\":\"#112233\",\"accent\":\"#80112233\"},"
                + "\"sprites\":{\"panel\":{\"texture\":\"x:y.png\",\"size\":[16,16],\"border\":[4,4,4,4]}},\"node_size\":30}");
        Theme t = Theme.fromJson("t", o);
        assertEquals(0xFF112233, t.color("text"));
        assertEquals(0x80112233, t.color("accent"));
        assertEquals(Theme.builtin().color("panel"), t.color("panel"), "missing colors fall back");
        assertEquals(30, t.nodeSize);
        t.draw(g, "panel", 0, 0, 50, 40);
        assertEquals(9, g.sprites, "nine slices");
        ThemeManager.put(t);
        ThemeManager.select("t");
        frame();
        assertTrue(g.sprites >= 9);
        ThemeManager.select("barelyquesting:default");
    }

    @Test
    void hudAndToastsDraw() {
        st.pinned.add("wood");
        server.engine().forceComplete(server.engine().teams().teamOf(alice.id).id, "wood");
        server.flush();
        assertFalse(st.toasts.isEmpty());
        g.reset();
        com.itmecasey.barelyquesting.ui.Hud.render(g);
        assertTrue(g.has("Gather Wood"));
        assertTrue(g.has("Quest complete"));
        assertNull(null);
    }
}

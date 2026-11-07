package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.progress.QuestEngine.Status;
import com.itmecasey.barelyquesting.theme.Theme;
import com.itmecasey.barelyquesting.theme.ThemeManager;

/**
 * The quest book screen, with no Minecraft types. A version module's {@code QuestScreen} forwards render and input here.
 * Layout: header (title and buttons), chapter list on the left, the graph or list in the middle, the selected quest on the right.
 */
public final class BookUi {
    public static final int HEADER = 24;
    public static final int SIDEBAR = 124;

    final ClientState st = ClientState.get();
    final Ui ui = new Ui();
    final GraphView graph = new GraphView(this);
    final ListView list = new ListView(this);
    final QuestPanel panel = new QuestPanel(this);

    public String chapterId = "";
    public String selected = "";
    public boolean editing;
    Modals.Modal modal;
    TextEditor focus;
    int sidebarScroll;
    String linkFrom;
    String pendingSelect;

    int bx;
    int by;
    int bw;
    int bh;
    int mainX;
    int mainY;
    int mainW;
    int mainH;
    int panelX;
    int panelW;

    public BookUi() {
        chapterId = st.lastChapter;
    }

    public ClientState state() {
        return st;
    }

    /** Presses a button of the last frame by its label (dev tour and tests). */
    public boolean press(String label) {
        return ui.press(label);
    }

    public Ui ui() {
        return ui;
    }

    public Chapter chapter() {
        Chapter c = st.book.chapter(chapterId);
        if (c == null && !st.book.chapters.isEmpty()) {
            c = st.book.chapters.get(0);
            chapterId = c.id;
        }
        return c;
    }

    public Quest selectedQuest() {
        return selected.isEmpty() ? null : st.book.quest(selected);
    }

    public void select(String questId) {
        if (!questId.equals(selected)) {
            panel.reset();
        }
        selected = questId;
        Chapter c = st.book.chapterOf(questId);
        if (c != null) {
            chapterId = c.id;
            st.lastChapter = c.id;
        }
    }

    public void openOn(String questId) {
        if (questId != null && !questId.isEmpty() && st.book.quest(questId) != null) {
            select(questId);
            graph.centerOn(st.book.quest(questId));
        }
    }

    public void open(Modals.Modal m) {
        modal = m;
        setFocus(null);
    }

    public void closeModal() {
        modal = null;
    }

    public boolean hasModal() {
        return modal != null;
    }

    void setFocus(TextEditor e) {
        if (focus != null && focus != e) {
            focus.blur();
        }
        focus = e;
        if (e != null) {
            e.focus();
        }
    }

    public boolean typing() {
        return focus != null && focus.focused;
    }

    /** Sends a themed message to the status line. */
    void say(String text, boolean error) {
        st.note = text;
        st.messageIsError = error;
        st.messageAt = ClientEnv.Holder.get().millis();
    }

    // ---- drawing ----

    public void render(Gfx g, int mouseX, int mouseY) {
        Theme th = ThemeManager.current();
        long now = ClientEnv.Holder.get().millis();
        ui.begin(g, th, mouseX, mouseY, now);
        if (st.pendingSound != null) {
            g.sound(st.pendingSound, 1.0f);
            st.pendingSound = null;
        }
        if (editing && !st.canEdit) {
            editing = false;
        }
        int sw = g.width();
        int sh = g.height();
        bx = Math.max(6, sw / 40);
        by = Math.max(6, sh / 30);
        bw = sw - bx * 2;
        bh = sh - by * 2;
        g.fill(0, 0, sw, sh, th.color("background"));
        th.draw(g, "panel", bx, by, bw, bh);
        if (pendingSelect != null && st.book.quest(pendingSelect) != null) {
            select(pendingSelect);
            pendingSelect = null;
        }
        Chapter chapter = chapter();
        boolean showPanel = selectedQuest() != null;
        panelW = showPanel ? Math.min(210, Math.max(150, bw / 3)) : 0;
        mainX = bx + SIDEBAR;
        mainY = by + HEADER;
        mainW = bw - SIDEBAR - panelW;
        mainH = bh - HEADER;
        panelX = mainX + mainW;

        header(g, th);
        sidebar(g, th);
        if (chapter == null) {
            g.centeredText("This book has no chapters yet.", mainX + mainW / 2, mainY + 40, th.color("text_muted"), 0);
            if (st.canEdit) {
                g.centeredText("Turn on Edit to add one.", mainX + mainW / 2, mainY + 54, th.color("text_muted"), 0);
            }
        } else {
            int top = mainY;
            if (editing) {
                toolbar(g, th, mainX + 2, top + 2, mainW - 4);
                top += 22;
            }
            int h = mainY + mainH - top - 2;
            if (st.view.equals("list")) {
                list.render(chapter, mainX + 2, top, mainW - 4, h);
            } else {
                graph.render(chapter, mainX + 2, top, mainW - 4, h);
            }
        }
        if (showPanel) {
            panel.render(panelX, mainY, panelW - 3, mainH - 3);
        }
        statusLine(g, th);
        if (modal != null) {
            ui.clearZones();
            g.fill(0, 0, sw, sh, 0x90000000);
            modal.render(this, ui);
        }
        Hud.renderToasts(ui);
        ui.end();
    }

    private void header(Gfx g, Theme th) {
        th.draw(g, "header", bx + 2, by + 2, bw - 4, HEADER - 4);
        g.text(ui.fit(st.book.title, 160, Gfx.BOLD), bx + 8, by + 8, th.color("title"), Gfx.BOLD | Gfx.SHADOW);
        int x = bx + bw - 6;
        int y = by + 4;
        int h = HEADER - 8;
        x -= 18;
        ui.button("X", x, y, 18, h, true, () -> closeRequested = true);
        x -= 52;
        ui.button("Themes", x, y, 50, h, true, () -> open(new Modals.ThemePick()));
        x -= 44;
        ui.button("Team", x, y, 42, h, true, () -> open(new Modals.TeamPanel()));
        if (st.canEdit) {
            x -= 44;
            ui.toggle("Edit", x, y, 42, h, editing, () -> {
                editing = !editing;
                linkFrom = null;
            });
        }
        x -= 50;
        ui.toggle("List", x, y, 48, h, st.view.equals("list"), () -> {
            st.view = "list";
            st.saveSettings();
        });
        x -= 52;
        ui.toggle("Graph", x, y, 50, h, !st.view.equals("list"), () -> {
            st.view = "graph";
            st.saveSettings();
        });
    }

    boolean closeRequested;

    public boolean consumeCloseRequest() {
        boolean c = closeRequested;
        closeRequested = false;
        return c;
    }

    private void sidebar(Gfx g, Theme th) {
        int x = bx + 3;
        int y = mainY;
        int w = SIDEBAR - 4;
        int h = mainH - 3 - (editing ? 24 : 0);
        th.draw(g, "panel_inset", x, y, w, mainH - 3);
        ui.clip(x + 1, y + 1, w - 2, h - 2);
        int rowH = 24;
        int cy = y + 2 - sidebarScroll;
        for (Chapter c : st.book.chapters) {
            boolean sel = c.id.equals(chapterId);
            int done = 0;
            int total = 0;
            for (Quest q : c.quests) {
                if (!st.isVisible(q)) {
                    continue;
                }
                total++;
                Status s = st.status(q);
                if (s == Status.COMPLETE || s == Status.CLAIMED) {
                    done++;
                }
            }
            th.draw(g, sel ? "tab_selected" : (ui.hover(x + 2, cy, w - 4, rowH - 2) ? "button_hover" : "tab"), x + 2, cy, w - 4, rowH - 2);
            g.item(c.icon, x + 5, cy + 3, 16);
            g.text(ui.fit(c.title, w - 30, 0), x + 24, cy + 3, th.color(sel ? "title" : "text"), 0);
            String count = done + "/" + total;
            g.text(count, x + 24, cy + 12, done == total && total > 0 ? th.color("success") : th.color("text_muted"), 0);
            final String id = c.id;
            ui.zone(x + 2, cy, w - 4, rowH - 2, b -> {
                g.sound(th.sound("click"), 1.0f);
                if (id.equals(chapterId) && editing && b == 0) {
                    open(new Modals.ChapterEdit(id));
                    return;
                }
                chapterId = id;
                st.lastChapter = id;
                selected = "";
                panel.reset();
                graph.centerOn(null);
                st.saveSettings();
            }, d -> sidebarScroll = Math.max(0, sidebarScroll - (int) (d * 12)));
            cy += rowH;
        }
        ui.unclip();
        if (editing) {
            ui.button("+ Chapter", x + 2, y + mainH - 3 - 22, w - 4, 20, true, () -> open(new Modals.Prompt("New chapter", "", false, name -> {
                if (name.isBlank()) {
                    return;
                }
                Chapter c = new Chapter(st.book.newChapterId(name));
                c.title = name.trim();
                c.icon = ItemSpec.of("minecraft:book");
                st.edit(EditOps.putChapter(c, null));
                chapterId = c.id;
            })));
        }
    }

    private void toolbar(Gfx g, Theme th, int x, int y, int w) {
        int h = 18;
        ui.button("+ Quest", x, y, 54, h, chapter() != null, () -> graph.addQuestAtCenter());
        x += 58;
        ui.button("Undo", x, y, 38, h, st.canUndo(), st::undo);
        x += 42;
        ui.button("Redo", x, y, 38, h, st.canRedo(), st::redo);
        x += 42;
        ui.toggle(linkFrom == null ? "Link" : "Linking...", x, y, 64, h, linkFrom != null, () -> {
            if (linkFrom != null) {
                linkFrom = null;
            } else if (!selected.isEmpty()) {
                linkFrom = selected;
                say("Click the quest that should depend on the selected one", false);
            } else {
                say("Select a quest first", true);
            }
        });
        x += 68;
        ui.button("Import...", x, y, 56, h, true, () -> say("Use /barelyquesting import <file or folder>", false));
    }

    private void statusLine(Gfx g, Theme th) {
        long age = ClientEnv.Holder.get().millis() - st.messageAt;
        if (st.note.isEmpty() || age > 6000) {
            return;
        }
        int w = Math.min(bw - 20, g.textWidth(st.note, 0) + 12);
        int x = bx + (bw - w) / 2;
        int y = by + bh - 20;
        g.fill(x, y, w, 15, 0xE0000000);
        g.outline(x, y, w, 15, st.messageIsError ? th.color("danger") : th.color("panel_edge"));
        g.text(ui.fit(st.note, w - 8, 0), x + 5, y + 4, st.messageIsError ? th.color("danger") : th.color("text"), 0);
    }

    // ---- input ----

    public boolean mouseClicked(int x, int y, int button) {
        ui.mx = x;
        ui.my = y;
        if (modal == null && focus != null) {
            // A click outside the focused field commits it; the click itself still goes to whatever was hit.
            if (!panel.ownsFocusAt(x, y)) {
                setFocus(null);
            }
        }
        return ui.click(x, y, button);
    }

    public boolean mouseDragged(int x, int y, int button) {
        ui.mx = x;
        ui.my = y;
        if (modal != null) {
            return false;
        }
        return graph.dragged(x, y);
    }

    public boolean mouseReleased(int x, int y, int button) {
        return graph.released(x, y);
    }

    public boolean mouseScrolled(int x, int y, double amount) {
        ui.mx = x;
        ui.my = y;
        return ui.scroll(x, y, amount);
    }

    /** Returns true if the key was used. Escape closes layers one at a time. */
    public boolean keyPressed(Gfx g, int key, int mods) {
        if (modal != null) {
            if (modal.key(this, g, key, mods)) {
                return true;
            }
            if (key == Keys.ESCAPE) {
                closeModal();
                return true;
            }
            return true;
        }
        if (focus != null && focus.focused) {
            return focus.keyPressed(key, mods, g);
        }
        boolean ctrl = (mods & Keys.MOD_CTRL) != 0;
        if (editing && ctrl && key == Keys.Z) {
            st.undo();
            return true;
        }
        if (editing && ctrl && key == Keys.Y) {
            st.redo();
            return true;
        }
        if (key == Keys.ESCAPE) {
            if (linkFrom != null) {
                linkFrom = null;
                return true;
            }
            if (!selected.isEmpty()) {
                selected = "";
                panel.reset();
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean charTyped(char c) {
        if (modal != null) {
            return modal.chr(c);
        }
        return focus != null && focus.charTyped(c);
    }

    /** Called when the screen closes. */
    public void closed() {
        setFocus(null);
        st.saveSettings();
    }
}

package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.progress.QuestEngine.Status;
import com.itmecasey.barelyquesting.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The same chapter as a list: a checkbox per quest, grouped by status, with a search box. */
final class ListView {
    private static final int ROW = 22;

    private final BookUi book;
    private final TextEditor search = new TextEditor(false);
    private int scroll;

    ListView(BookUi book) {
        this.book = book;
        search.placeholder = "Search quests";
        search.maxLength = 60;
    }

    private static final Status[] ORDER = {Status.COMPLETE, Status.IN_PROGRESS, Status.AVAILABLE, Status.CLAIMED, Status.LOCKED};

    void render(Chapter c, int x, int y, int w, int h) {
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        th.draw(g, "panel_inset", x, y, w, h);

        // search box
        int sx = x + 4;
        int sy = y + 4;
        int sw = w - 8;
        th.draw(g, "slot", sx, sy, sw, 16);
        search.draw(g, sx, sy, sw, 16, th.color("text"), th.color("text_muted"), ui.now);
        ui.zone(sx, sy, sw, 16, b -> {
            book.setFocus(search);
            search.click(g, ui.mx - sx, ui.my - sy);
        }, null);

        String filter = search.text().trim().toLowerCase(Locale.ROOT);
        List<Object> rows = new ArrayList<>(); // String header or Quest
        for (Status s : ORDER) {
            List<Quest> group = new ArrayList<>();
            for (Quest q : c.quests) {
                if (!book.st.isVisible(q) || book.st.status(q) != s) {
                    continue;
                }
                if (!filter.isEmpty() && !q.title.toLowerCase(Locale.ROOT).contains(filter)) {
                    continue;
                }
                group.add(q);
            }
            if (!group.isEmpty()) {
                rows.add(label(s) + " (" + group.size() + ")");
                rows.addAll(group);
            }
        }
        int top = sy + 22;
        int viewH = h - (top - y) - 4;
        int contentH = rows.size() * ROW;
        scroll = Ui.clamp(scroll, 0, Math.max(0, contentH - viewH));
        ui.clip(x + 2, top, w - 4, viewH);
        ui.zone(x + 2, top, w - 4, viewH, null, d -> scroll = Ui.clamp(scroll - (int) (d * 20), 0, Math.max(0, contentH - viewH)));
        int ry = top - scroll;
        for (Object row : rows) {
            if (row instanceof String) {
                g.text((String) row, x + 8, ry + 7, th.color("title"), Gfx.BOLD);
                g.fill(x + 6, ry + ROW - 3, w - 16, 1, th.color("panel_edge"));
            } else {
                Quest q = (Quest) row;
                quest(g, th, ui, q, x + 4, ry, w - 14);
            }
            ry += ROW;
        }
        ui.unclip();
        ui.scrollbar(x + w - 6, top, viewH, viewH, contentH, scroll);
        if (rows.isEmpty()) {
            g.text(filter.isEmpty() ? "" : "No quest matches.", x + 10, top + 8, th.color("text_muted"), 0);
        }
    }

    private static String label(Status s) {
        switch (s) {
            case COMPLETE:
                return "Ready to claim";
            case IN_PROGRESS:
                return "In progress";
            case AVAILABLE:
                return "Available";
            case CLAIMED:
                return "Done";
            default:
                return "Locked";
        }
    }

    private void quest(Gfx g, Theme th, Ui ui, Quest q, int x, int y, int w) {
        Status s = book.st.status(q);
        boolean sel = q.id.equals(book.selected);
        boolean over = ui.hover(x, y, w, ROW - 2);
        if (sel || over) {
            g.fill(x, y, w, ROW - 2, sel ? th.color("tab_selected") : th.color("button"));
        }
        boolean done = s == Status.COMPLETE || s == Status.CLAIMED;
        ui.checkbox(x + 3, y + 5, 11, done, th.color("success"));
        g.item(q.icon, x + 19, y + 2, 16);
        int tc = s == Status.LOCKED ? th.color("text_muted") : th.color("text");
        String right = progressText(q);
        int rw = g.textWidth(right, 0);
        g.text(ui.fit(q.title, w - 44 - rw, 0), x + 40, y + 7, tc, done ? Gfx.STRIKE & 0 : 0);
        g.text(right, x + w - rw - 4, y + 7, th.color("text_muted"), 0);
        ui.zone(x, y, w, ROW - 2, b -> {
            g.sound(th.sound("click"), 1.0f);
            book.select(q.id);
        }, null);
    }

    private String progressText(Quest q) {
        long have = 0;
        long need = 0;
        for (Def t : q.tasks) {
            have += Math.min(book.st.taskProgress(q, t), QuestEngine.targetOf(t));
            need += QuestEngine.targetOf(t);
        }
        if (q.tasks.isEmpty()) {
            return "";
        }
        return q.tasks.size() == 1 && need > 1 ? have + "/" + need : (int) Math.round(100.0 * have / Math.max(1, need)) + "%";
    }
}

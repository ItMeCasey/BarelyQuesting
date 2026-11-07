package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.progress.QuestEngine.Status;
import com.itmecasey.barelyquesting.theme.Theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The pannable, zoomable node graph of one chapter: dependency lines, nodes by status, and (when editing) dragging and linking. */
final class GraphView {
    /** Pixels between two grid cells at zoom 1. */
    static final int CELL = 30;

    private final BookUi book;
    private final Map<String, double[]> view = new HashMap<>(); // chapter id -> panX, panY, zoom
    private int vx;
    private int vy;
    private int vw;
    private int vh;
    private Chapter current;

    private enum Drag { NONE, PAN, NODE }

    private Drag drag = Drag.NONE;
    private String dragQuest = "";
    private int dragStartX;
    private int dragStartY;
    private double dragPanX;
    private double dragPanY;
    private int dragNodeX;
    private int dragNodeY;
    private boolean dragMoved;
    private boolean center;

    GraphView(BookUi book) {
        this.book = book;
    }

    private double[] v(Chapter c) {
        return view.computeIfAbsent(c.id, k -> new double[] {Double.NaN, 0, 1});
    }

    void centerOn(Quest q) {
        center = true;
        if (q != null) {
            Chapter c = book.st.book.chapterOf(q.id);
            if (c != null) {
                double[] v = v(c);
                v[0] = -q.x * CELL * v[2];
                v[1] = -q.y * CELL * v[2];
                center = false;
            }
        } else if (current != null) {
            v(current)[0] = Double.NaN;
        }
    }

    private void ensureView(Chapter c) {
        double[] v = v(c);
        if (Double.isNaN(v[0])) {
            if (c.quests.isEmpty()) {
                v[0] = 0;
                v[1] = 0;
            } else {
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                for (Quest q : c.quests) {
                    minX = Math.min(minX, q.x);
                    maxX = Math.max(maxX, q.x);
                    minY = Math.min(minY, q.y);
                    maxY = Math.max(maxY, q.y);
                }
                double needW = (maxX - minX) * CELL + book.ui.th.nodeSize + 24;
                double needH = (maxY - minY) * CELL + book.ui.th.nodeSize + 24;
                v[2] = Math.max(0.5, Math.min(1.0, Math.min(vw / needW, vh / needH)));
                v[0] = -(minX + maxX) / 2.0 * CELL * v[2];
                v[1] = -(minY + maxY) / 2.0 * CELL * v[2];
            }
        }
    }

    private int px(double[] v, int cellX) {
        return (int) Math.round(vx + vw / 2.0 + v[0] + cellX * CELL * v[2]);
    }

    private int py(double[] v, int cellY) {
        return (int) Math.round(vy + vh / 2.0 + v[1] + cellY * CELL * v[2]);
    }

    void render(Chapter c, int x, int y, int w, int h) {
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        current = c;
        vx = x;
        vy = y;
        vw = w;
        vh = h;
        ensureView(c);
        double[] v = v(c);
        th.draw(g, "panel_inset", x, y, w, h);
        ui.clip(x + 1, y + 1, w - 2, h - 2);
        int size = Math.max(14, (int) Math.round(th.nodeSize * v[2]));

        // grid dots in edit mode
        if (book.editing) {
            int step = Math.max(8, (int) (CELL * v[2]));
            int ox = Math.floorMod(px(v, 0) - x, step);
            int oy = Math.floorMod(py(v, 0) - y, step);
            for (int gx = x + ox; gx < x + w; gx += step) {
                for (int gy = y + oy; gy < y + h; gy += step) {
                    g.fill(gx, gy, 1, 1, 0x40FFFFFF);
                }
            }
        }

        // background zone: pan, add quest on right-click when editing
        ui.zone(x, y, w, h, b -> pressed(c, b), d -> zoom(c, d));

        // lines
        for (Quest q : c.quests) {
            if (!book.st.isVisible(q)) {
                continue;
            }
            for (String dep : q.dependencies) {
                Quest d = book.st.book.quest(dep);
                if (d == null || !book.st.isVisible(d)) {
                    continue;
                }
                boolean done = book.st.engine().isComplete(book.st.teamId, dep);
                line(g, th, v, size, d, q, done ? th.color("line_done") : th.color("line"));
            }
        }
        // nodes
        List<Quest> hoverList = new ArrayList<>();
        for (Quest q : c.quests) {
            if (!book.st.isVisible(q)) {
                continue;
            }
            int cx = px(v, q.x);
            int cy = py(v, q.y);
            if (drag == Drag.NODE && dragMoved && q.id.equals(dragQuest)) {
                cx = dragNodeX;
                cy = dragNodeY;
            }
            if (cx + size < x || cy + size < y || cx - size > x + w || cy - size > y + h) {
                continue;
            }
            Status s = book.st.status(q);
            int nx = cx - size / 2;
            int ny = cy - size / 2;
            th.draw(g, "node_" + s.name().toLowerCase(java.util.Locale.ROOT), nx, ny, size, size);
            int icon = Math.max(10, size * 3 / 5);
            g.item(q.icon, cx - icon / 2, cy - icon / 2, icon);
            if (q.id.equals(book.selected)) {
                g.outline(nx - 2, ny - 2, size + 4, size + 4, th.color("node_selected"));
            }
            if (q.id.equals(book.linkFrom)) {
                g.outline(nx - 3, ny - 3, size + 6, size + 6, th.color("accent"));
            }
            if (s == Status.COMPLETE) {
                int pulse = (ui.now / 400) % 2 == 0 ? th.color("accent") : th.color("title");
                g.fill(nx + size - 7, ny - 2, 9, 9, 0xFF101015);
                g.fill(nx + size - 6, ny - 1, 7, 7, pulse);
            }
            if (q.optional) {
                g.fill(nx - 1, ny - 1, 5, 5, th.color("text_muted"));
            }
            if (ui.hover(nx, ny, size, size)) {
                hoverList.add(q);
            }
        }
        ui.unclip();
        // zone for nodes: topmost, so registered after the background; click handled by hit test
        ui.zone(x, y, w, h, b -> pressed(c, b), d -> zoom(c, d));

        if (!hoverList.isEmpty() && drag != Drag.PAN) {
            Quest q = hoverList.get(hoverList.size() - 1);
            g.tooltip(tooltip(q), ui.mx, ui.my);
        }
        g.text(Math.round(v[2] * 100) + "%", x + w - 34, y + h - 11, th.color("text_muted"), 0);
    }

    private List<String> tooltip(Quest q) {
        List<String> t = new ArrayList<>();
        t.add(q.title);
        Status s = book.st.status(q);
        t.add(s.name().substring(0, 1) + s.name().substring(1).toLowerCase(java.util.Locale.ROOT));
        for (Def task : q.tasks) {
            long have = book.st.taskProgress(q, task);
            long need = QuestEngine.targetOf(task);
            if (need > 1) {
                t.add(have + "/" + need);
            }
        }
        if (book.editing) {
            t.add("Drag to move");
        }
        return t;
    }

    private void line(Gfx g, Theme th, double[] v, int size, Quest from, Quest to, int color) {
        int ax = px(v, from.x);
        int ay = py(v, from.y);
        int bx = px(v, to.x);
        int by = py(v, to.y);
        int half = size / 2;
        int t = Math.max(2, (int) Math.round(2 * v[2]));
        if (bx - ax > size) {
            int midX = (ax + bx) / 2;
            hline(g, ax + half, midX, ay, t, color);
            vline(g, midX, ay, by, t, color);
            hline(g, midX, bx - half - 3, by, t, color);
            g.fill(bx - half - 4, by - 2, 2, 5, color);
            g.fill(bx - half - 2, by - 1, 2, 3, color);
        } else if (bx - ax < -size) {
            int midX = (ax + bx) / 2;
            hline(g, midX, ax - half, ay, t, color);
            vline(g, midX, ay, by, t, color);
            hline(g, bx + half + 3, midX, by, t, color);
            g.fill(bx + half + 2, by - 2, 2, 5, color);
            g.fill(bx + half, by - 1, 2, 3, color);
        } else {
            int dir = by >= ay ? 1 : -1;
            vline(g, ax, ay + dir * half, by - dir * (half + 3), t, color);
            if (ax != bx) {
                hline(g, ax, bx, by - dir * (half + 3), t, color);
            }
            g.fill(bx - 2, by - dir * (half + 4), 5, 2, color);
            g.fill(bx - 1, by - dir * (half + 2), 3, 2, color);
        }
    }

    private static void hline(Gfx g, int x1, int x2, int y, int t, int color) {
        g.fill(Math.min(x1, x2), y - t / 2, Math.abs(x2 - x1) + 1, t, color);
    }

    private static void vline(Gfx g, int x, int y1, int y2, int t, int color) {
        g.fill(x - t / 2, Math.min(y1, y2), t, Math.abs(y2 - y1) + 1, color);
    }

    private Quest nodeAt(Chapter c, int mx, int my) {
        double[] v = v(c);
        int size = Math.max(14, (int) Math.round(book.ui.th.nodeSize * v[2]));
        Quest hit = null;
        for (Quest q : c.quests) {
            if (!book.st.isVisible(q)) {
                continue;
            }
            int cx = px(v, q.x);
            int cy = py(v, q.y);
            if (mx >= cx - size / 2 && mx < cx + size / 2 && my >= cy - size / 2 && my < cy + size / 2) {
                hit = q;
            }
        }
        return hit;
    }

    private void pressed(Chapter c, int button) {
        Ui ui = book.ui;
        Quest hit = nodeAt(c, ui.mx, ui.my);
        if (hit != null) {
            ui.g.sound(ui.th.sound("click"), 1.0f);
            if (book.linkFrom != null && book.editing) {
                toggleLink(book.linkFrom, hit);
                book.linkFrom = null;
                return;
            }
            if (button == 1) {
                book.select(hit.id);
                return;
            }
            book.select(hit.id);
            if (book.editing) {
                drag = Drag.NODE;
                dragQuest = hit.id;
                dragStartX = ui.mx;
                dragStartY = ui.my;
                dragMoved = false;
                dragNodeX = px(v(c), hit.x);
                dragNodeY = py(v(c), hit.y);
            }
            return;
        }
        if (book.editing && button == 1) {
            double[] v = v(c);
            int cellX = (int) Math.round((ui.mx - px(v, 0)) / (CELL * v[2]));
            int cellY = (int) Math.round((ui.my - py(v, 0)) / (CELL * v[2]));
            newQuest(c, cellX, cellY);
            return;
        }
        if (button == 0 && book.linkFrom != null) {
            book.linkFrom = null;
        }
        drag = Drag.PAN;
        dragStartX = ui.mx;
        dragStartY = ui.my;
        double[] v = v(c);
        dragPanX = v[0];
        dragPanY = v[1];
    }

    boolean dragged(int mx, int my) {
        if (current == null || drag == Drag.NONE) {
            return false;
        }
        double[] v = v(current);
        if (drag == Drag.PAN) {
            v[0] = dragPanX + (mx - dragStartX);
            v[1] = dragPanY + (my - dragStartY);
            return true;
        }
        if (Math.abs(mx - dragStartX) + Math.abs(my - dragStartY) > 3) {
            dragMoved = true;
        }
        if (dragMoved) {
            Quest q = book.st.book.quest(dragQuest);
            if (q != null) {
                dragNodeX = px(v, q.x) + (mx - dragStartX);
                dragNodeY = py(v, q.y) + (my - dragStartY);
            }
        }
        return true;
    }

    boolean released(int mx, int my) {
        Drag d = drag;
        drag = Drag.NONE;
        if (d == Drag.NODE && dragMoved && current != null) {
            Quest q = book.st.book.quest(dragQuest);
            double[] v = v(current);
            if (q != null) {
                int cx = (int) Math.round((dragNodeX - px(v, 0)) / (CELL * v[2]));
                int cy = (int) Math.round((dragNodeY - py(v, 0)) / (CELL * v[2]));
                if (cx != q.x || cy != q.y) {
                    Quest moved = q.copy();
                    moved.x = cx;
                    moved.y = cy;
                    book.st.edit(EditOps.putQuest(current.id, moved, null));
                }
            }
        }
        dragMoved = false;
        return d != Drag.NONE;
    }

    private void zoom(Chapter c, double amount) {
        double[] v = v(c);
        double old = v[2];
        double nz = Math.max(0.5, Math.min(2.0, old * Math.pow(1.12, amount)));
        // keep the point under the mouse fixed
        double mxRel = book.ui.mx - (vx + vw / 2.0 + v[0]);
        double myRel = book.ui.my - (vy + vh / 2.0 + v[1]);
        v[0] -= mxRel * (nz / old - 1);
        v[1] -= myRel * (nz / old - 1);
        v[2] = nz;
    }

    // ---- editing helpers ----

    void addQuestAtCenter() {
        Chapter c = book.chapter();
        if (c == null) {
            return;
        }
        double[] v = v(c);
        int cellX = (int) Math.round(-v[0] / (CELL * v[2]));
        int cellY = (int) Math.round(-v[1] / (CELL * v[2]));
        while (occupied(c, cellX, cellY)) {
            cellX++;
        }
        newQuest(c, cellX, cellY);
    }

    private boolean occupied(Chapter c, int x, int y) {
        for (Quest q : c.quests) {
            if (q.x == x && q.y == y) {
                return true;
            }
        }
        return false;
    }

    private void newQuest(Chapter c, int cellX, int cellY) {
        Quest q = new Quest(book.st.book.newQuestId());
        q.title = "New Quest";
        q.icon = ItemSpec.of("minecraft:book");
        q.x = cellX;
        q.y = cellY;
        q.tasks.add(Def.of("t0", "checkmark").with("text", "Check this off"));
        book.st.edit(EditOps.putQuest(c.id, q, null));
        book.pendingSelect = q.id;
    }

    private void toggleLink(String from, Quest to) {
        if (from.equals(to.id)) {
            return;
        }
        Quest copy = to.copy();
        if (copy.dependencies.contains(from)) {
            copy.dependencies.remove(from);
        } else {
            if (book.st.book.reaches(from, to.id, new java.util.HashSet<>())) {
                book.say("That would make a loop", true);
                return;
            }
            copy.dependencies.add(from);
        }
        Chapter c = book.st.book.chapterOf(to.id);
        book.st.edit(EditOps.putQuest(c.id, copy, null));
    }
}

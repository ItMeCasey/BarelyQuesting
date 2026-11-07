package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.theme.Theme;
import com.itmecasey.barelyquesting.theme.ThemeManager;

import java.util.Iterator;

/** The in-game overlay: the pinned-quest tracker in the top left and themed toasts in the top right. */
public final class Hud {
    private static final long TOAST_MS = 5000;

    private Hud() {
    }

    /** Draws toasts over the book (the HUD overlay draws them when the book is closed). */
    public static void renderToasts(Ui ui) {
        drawToasts(ui.g, ui.th, ui.now);
    }

    public static void render(Gfx g) {
        ClientState st = ClientState.get();
        Theme th = ThemeManager.current();
        long now = ClientEnv.Holder.get().millis();
        g.font(th.font);
        if (st.pendingSound != null) {
            g.sound(st.pendingSound, 1.0f);
            st.pendingSound = null;
        }
        drawToasts(g, th, now);
        if (!st.hud || st.pinned.isEmpty() || !st.connected) {
            return;
        }
        int x = 6;
        int y = 6;
        int w = 150;
        for (String id : st.pinned) {
            Quest q = st.book.quest(id);
            if (q == null || !st.isVisible(q)) {
                continue;
            }
            QuestEngine.Status s = st.status(q);
            if (s == QuestEngine.Status.CLAIMED) {
                continue;
            }
            int h = 14 + 11 * Math.max(1, Math.min(3, q.tasks.size()));
            g.fill(x, y, w, h, 0x90000000);
            g.fill(x, y, 2, h, s == QuestEngine.Status.COMPLETE ? th.color("accent") : th.color("node_available"));
            g.item(q.icon, x + 4, y + 1, 10);
            g.text(trim(g, q.title, w - 22), x + 17, y + 3, th.color("title"), Gfx.SHADOW);
            int ty = y + 14;
            int shown = 0;
            for (Def t : q.tasks) {
                if (shown >= 3) {
                    break;
                }
                long have = st.taskProgress(q, t);
                long need = QuestEngine.targetOf(t);
                if (have >= need && q.tasks.size() > 1) {
                    continue;
                }
                String text = TaskText.task(g, t);
                String num = need > 1 ? " " + have + "/" + need : (have >= need ? " done" : "");
                g.text(trim(g, text, w - 8 - g.textWidth(num, 0)) + num, x + 5, ty, have >= need ? th.color("success") : th.color("text"), Gfx.SHADOW);
                ty += 11;
                shown++;
            }
            if (s == QuestEngine.Status.COMPLETE) {
                g.text("Ready to claim!", x + 5, ty, th.color("accent"), Gfx.SHADOW);
            }
            y += h + 3;
        }
    }

    private static String trim(Gfx g, String s, int w) {
        if (g.textWidth(s, 0) <= w) {
            return s;
        }
        int n = s.length();
        while (n > 0 && g.textWidth(s.substring(0, n) + "..", 0) > w) {
            n--;
        }
        return s.substring(0, n) + "..";
    }

    private static void drawToasts(Gfx g, Theme th, long now) {
        ClientState st = ClientState.get();
        Iterator<ClientState.Toast> it = st.toasts.iterator();
        while (it.hasNext()) {
            if (now - it.next().shownAt > TOAST_MS) {
                it.remove();
            }
        }
        int y = 6;
        int w = 160;
        for (ClientState.Toast t : st.toasts) {
            long age = now - t.shownAt;
            int slide = age < 250 ? (int) ((250 - age) * w / 250) : (age > TOAST_MS - 250 ? (int) ((age - (TOAST_MS - 250)) * w / 250) : 0);
            int x = g.width() - w - 6 + slide;
            th.draw(g, "panel", x, y, w, 30);
            g.text(trim(g, t.text, w - 12), x + 6, y + 6, th.color("accent"), 0);
            g.text(trim(g, t.title, w - 12), x + 6, y + 17, th.color("title"), Gfx.SHADOW);
            y += 33;
        }
    }
}

package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Per-frame drawing helpers with click zones. Everything drawn through the helpers registers a zone; input is matched against the zones
 * of the last finished frame, topmost (last drawn) first. A modal clears the zones below it, so nothing underneath reacts.
 */
public final class Ui {
    /** A rectangle that reacts to the mouse. */
    public static final class Zone {
        final int x;
        final int y;
        final int w;
        final int h;
        final IntConsumer click;
        final DoubleConsumer scroll;

        Zone(int x, int y, int w, int h, IntConsumer click, DoubleConsumer scroll) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.click = click;
            this.scroll = scroll;
        }

        boolean contains(int mx, int my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    public Gfx g;
    public Theme th;
    public int mx;
    public int my;
    public long now;
    private List<Zone> zones = new ArrayList<>();
    private List<Zone> last = new ArrayList<>();
    private final List<int[]> clips = new ArrayList<>();
    private java.util.Map<String, int[]> labels = new java.util.HashMap<>();
    private java.util.Map<String, int[]> lastLabels = new java.util.HashMap<>();

    public void begin(Gfx gfx, Theme theme, int mouseX, int mouseY, long millis) {
        this.g = gfx;
        this.th = theme;
        this.mx = mouseX;
        this.my = mouseY;
        this.now = millis;
        zones = new ArrayList<>();
        labels = new java.util.HashMap<>();
        clips.clear();
        g.font(theme.font);
    }

    public void end() {
        last = zones;
        lastLabels = labels;
    }

    /** Drops every zone registered so far (a modal calls this before drawing itself). */
    public void clearZones() {
        zones.clear();
    }

    public boolean hover(int x, int y, int w, int h) {
        if (mx < x || my < y || mx >= x + w || my >= y + h) {
            return false;
        }
        for (int[] c : clips) {
            if (mx < c[0] || my < c[1] || mx >= c[0] + c[2] || my >= c[1] + c[3]) {
                return false;
            }
        }
        return true;
    }

    public void clip(int x, int y, int w, int h) {
        g.clipPush(x, y, w, h);
        clips.add(new int[] {x, y, w, h});
    }

    public void unclip() {
        g.clipPop();
        clips.remove(clips.size() - 1);
    }

    /** Registers a click (and optional scroll) zone, limited by the active clip rectangles. */
    public void zone(int x, int y, int w, int h, IntConsumer click, DoubleConsumer scroll) {
        int x0 = x;
        int y0 = y;
        int x1 = x + w;
        int y1 = y + h;
        for (int[] c : clips) {
            x0 = Math.max(x0, c[0]);
            y0 = Math.max(y0, c[1]);
            x1 = Math.min(x1, c[0] + c[2]);
            y1 = Math.min(y1, c[1] + c[3]);
        }
        if (x1 > x0 && y1 > y0) {
            zones.add(new Zone(x0, y0, x1 - x0, y1 - y0, click, scroll));
        }
    }

    public boolean click(int x, int y, int button) {
        for (int i = last.size() - 1; i >= 0; i--) {
            Zone z = last.get(i);
            if (z.contains(x, y) && z.click != null) {
                z.click.accept(button);
                return true;
            }
        }
        return false;
    }

    public boolean scroll(int x, int y, double amount) {
        for (int i = last.size() - 1; i >= 0; i--) {
            Zone z = last.get(i);
            if (z.contains(x, y) && z.scroll != null) {
                z.scroll.accept(amount);
                return true;
            }
        }
        return false;
    }

    // ---- widgets ----

    public int color(String key) {
        return th.color(key);
    }

    public void text(String s, int x, int y, String colorKey) {
        g.text(s, x, y, th.color(colorKey), 0);
    }

    public void text(String s, int x, int y, int argb, int style) {
        g.text(s, x, y, argb, style);
    }

    /** Text cut to a width with an ellipsis. */
    public String fit(String s, int maxWidth, int style) {
        if (g.textWidth(s, style) <= maxWidth) {
            return s;
        }
        String e = "...";
        int n = s.length();
        while (n > 0 && g.textWidth(s.substring(0, n) + e, style) > maxWidth) {
            n--;
        }
        return s.substring(0, n) + e;
    }

    /** Splits the text into lines no wider than maxWidth, breaking at spaces (and inside a word that is wider than a line). */
    public java.util.List<String> wrap(String s, int maxWidth, int style) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String tryLine = line.length() == 0 ? word : line + " " + word;
            if (g.textWidth(tryLine, style) <= maxWidth) {
                line.setLength(0);
                line.append(tryLine);
                continue;
            }
            if (line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            String rest = word;
            while (!rest.isEmpty() && g.textWidth(rest, style) > maxWidth) {
                int n = rest.length();
                while (n > 1 && g.textWidth(rest.substring(0, n), style) > maxWidth) {
                    n--;
                }
                lines.add(rest.substring(0, n));
                rest = rest.substring(n);
            }
            line.append(rest);
        }
        if (line.length() > 0 || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** Draws wrapped text and returns the y below its last line. */
    public int paragraph(String s, int x, int y, int maxWidth, int color, int style) {
        for (String l : wrap(s, maxWidth, style)) {
            g.text(l, x, y, color, style);
            y += g.lineHeight() + 1;
        }
        return y;
    }

    public boolean button(String label, int x, int y, int w, int h, boolean enabled, Runnable action) {
        boolean over = enabled && hover(x, y, w, h);
        th.draw(g, enabled ? (over ? "button_hover" : "button") : "button_off", x, y, w, h);
        String t = fit(label, w - 6, 0);
        g.text(t, x + (w - g.textWidth(t, 0)) / 2, y + (h - g.lineHeight()) / 2 + 1, enabled ? th.color("text") : th.color("text_muted"), 0);
        if (enabled && action != null) {
            labels.put(label, new int[] {x, y, w, h});
            zone(x, y, w, h, b -> {
                g.sound(th.sound("click"), 1.0f);
                action.run();
            }, null);
        }
        return over;
    }

    /** A toggle drawn as a button that is lit while on. */
    public void toggle(String label, int x, int y, int w, int h, boolean on, Runnable action) {
        boolean over = hover(x, y, w, h);
        th.draw(g, on ? "tab_selected" : (over ? "button_hover" : "button"), x, y, w, h);
        String t = fit(label, w - 6, 0);
        g.text(t, x + (w - g.textWidth(t, 0)) / 2, y + (h - g.lineHeight()) / 2 + 1, on ? th.color("title") : th.color("text"), 0);
        labels.put(label, new int[] {x, y, w, h});
        zone(x, y, w, h, b -> {
            g.sound(th.sound("click"), 1.0f);
            action.run();
        }, null);
    }

    /** Clicks the centre of a button or toggle of the last frame by its label (tests and the dev tour use this). */
    public boolean press(String label) {
        int[] r = lastLabels.get(label);
        if (r == null) {
            return false;
        }
        mx = r[0] + r[2] / 2;
        my = r[1] + r[3] / 2;
        return click(mx, my, 0);
    }

    public void checkbox(int x, int y, int size, boolean on, int markColor) {
        g.fill(x, y, size, size, th.color("panel_edge"));
        g.fill(x + 1, y + 1, size - 2, size - 2, th.color("slot"));
        if (on) {
            int s = size;
            g.fill(x + s / 4, y + s / 2, Math.max(2, s / 5), Math.max(2, s / 4), markColor);
            for (int i = 0; i < s / 2; i++) {
                g.fill(x + s / 4 + 1 + i, y + s / 2 + s / 5 - i, 2, 2, markColor);
            }
        }
    }

    public void bar(int x, int y, int w, int h, double fraction) {
        th.draw(g, "bar", x, y, w, h);
        int fw = (int) Math.round((w - 2) * Math.max(0, Math.min(1, fraction)));
        if (fw > 0) {
            g.fill(x + 1, y + 1, fw, h - 2, th.color("bar_fill"));
        }
    }

    public void slot(int x, int y, int size) {
        th.draw(g, "slot", x, y, size, size);
    }

    /** A vertical scrollbar for content taller than its view. */
    public void scrollbar(int x, int y, int h, int viewH, int contentH, int offset) {
        if (contentH <= viewH) {
            return;
        }
        g.fill(x, y, 3, h, th.color("slot"));
        int thumb = Math.max(10, h * viewH / contentH);
        int pos = (h - thumb) * offset / Math.max(1, contentH - viewH);
        g.fill(x, y + pos, 3, thumb, th.color("line"));
    }

    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

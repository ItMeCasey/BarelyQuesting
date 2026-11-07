package com.itmecasey.barelyquesting.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.ui.Gfx;

import java.util.HashMap;
import java.util.Map;

/**
 * Colors, sprites, a font and sounds for the book, read from a theme file in a resource pack. Anything a theme leaves out falls back
 * to the built-in values, so a theme can be as small as a few colors.
 *
 * <pre>
 * {
 *   "name": "Parchment", "author": "...", "font": "",
 *   "colors": { "background": "#e0f4ecd4", "text": "#3b2a1a", ... },
 *   "sprites": { "panel": { "texture": "barelyquesting:textures/gui/parchment/panel.png", "size": [32, 32], "border": [6, 6, 6, 6] } },
 *   "sounds": { "click": "minecraft:ui.button.click" },
 *   "node_size": 26
 * }
 * </pre>
 *
 * Sprite names: {@code panel}, {@code panel_inset}, {@code button}, {@code button_hover}, {@code button_off}, {@code tab},
 * {@code tab_selected}, {@code slot}, {@code bar}, {@code bar_fill}, {@code node_locked}, {@code node_available},
 * {@code node_progress}, {@code node_complete}, {@code node_claimed}, {@code node_selected}.
 */
public final class Theme {
    /** One sprite: a region of a texture with optional nine-slice borders. */
    public static final class Sprite {
        public final String texture;
        public final int u;
        public final int v;
        public final int w;
        public final int h;
        public final int texW;
        public final int texH;
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;

        Sprite(String texture, int u, int v, int w, int h, int texW, int texH, int l, int t, int r, int b) {
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.w = w;
            this.h = h;
            this.texW = texW;
            this.texH = texH;
            this.left = l;
            this.top = t;
            this.right = r;
            this.bottom = b;
        }

        static Sprite fromJson(JsonObject o) {
            String tex = Json.str(o, "texture", "");
            int[] size = ints(o, "size", 2, 16);
            int[] region = o.has("region") ? ints(o, "region", 4, 0) : new int[] {0, 0, size[0], size[1]};
            int[] border = ints(o, "border", 4, 0);
            return new Sprite(tex, region[0], region[1], region[2], region[3], size[0], size[1], border[0], border[1], border[2], border[3]);
        }

        private static int[] ints(JsonObject o, String key, int n, int def) {
            int[] out = new int[n];
            java.util.Arrays.fill(out, def);
            if (o.has(key) && o.get(key).isJsonArray()) {
                com.google.gson.JsonArray a = o.get(key).getAsJsonArray();
                for (int i = 0; i < n && i < a.size(); i++) {
                    out[i] = a.get(i).getAsInt();
                }
            }
            return out;
        }
    }

    public final String id;
    public String name;
    public String author = "";
    public String font = "";
    public int nodeSize = 26;
    private final Map<String, Integer> colors = new HashMap<>();
    private final Map<String, Sprite> sprites = new HashMap<>();
    private final Map<String, String> sounds = new HashMap<>();

    public Theme(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** The built-in theme: plain fills in a dark vanilla-like style. Also the fallback for every missing value. */
    public static Theme builtin() {
        Theme t = new Theme("builtin", "Default");
        t.color("background", 0xC0101018);
        t.color("panel", 0xFF2B2B33);
        t.color("panel_edge", 0xFF55556A);
        t.color("panel_inset", 0xFF1B1B22);
        t.color("header", 0xFF22222B);
        t.color("text", 0xFFEDEDF5);
        t.color("text_muted", 0xFFA0A0B4);
        t.color("title", 0xFFFFE28C);
        t.color("accent", 0xFFF2B33D);
        t.color("button", 0xFF3A3A48);
        t.color("button_hover", 0xFF4C4C60);
        t.color("button_off", 0xFF26262E);
        t.color("tab", 0xFF2B2B33);
        t.color("tab_selected", 0xFF45455A);
        t.color("slot", 0xFF15151B);
        t.color("bar", 0xFF15151B);
        t.color("bar_fill", 0xFF4FC36B);
        t.color("line", 0xFF6A6A80);
        t.color("line_done", 0xFF4FC36B);
        t.color("node_locked", 0xFF3A3A44);
        t.color("node_available", 0xFF4A6FA5);
        t.color("node_progress", 0xFFC9A227);
        t.color("node_complete", 0xFF4FC36B);
        t.color("node_claimed", 0xFF2E7D46);
        t.color("node_selected", 0xFFFFFFFF);
        t.color("danger", 0xFFE05252);
        t.color("success", 0xFF4FC36B);
        t.sounds.put("click", "minecraft:ui.button.click");
        t.sounds.put("complete", "minecraft:entity.player.levelup");
        t.sounds.put("claim", "minecraft:entity.experience_orb.pickup");
        t.sounds.put("open", "minecraft:item.book.page_turn");
        return t;
    }

    private static Theme fallback = builtin();

    public void color(String key, int argb) {
        colors.put(key, argb);
    }

    public int color(String key) {
        Integer c = colors.get(key);
        if (c != null) {
            return c;
        }
        Integer f = fallback.colors.get(key);
        return f == null ? 0xFFFF00FF : f;
    }

    public String sound(String key) {
        String s = sounds.get(key);
        return s != null ? s : fallback.sounds.get(key);
    }

    public Sprite sprite(String name) {
        return sprites.get(name);
    }

    public static Theme fromJson(String id, JsonObject o) {
        Theme t = new Theme(id, Json.str(o, "name", id));
        t.author = Json.str(o, "author", "");
        t.font = Json.str(o, "font", "");
        t.nodeSize = Math.max(16, Math.min(48, Json.integer(o, "node_size", 26)));
        JsonObject colors = Json.obj(o, "colors");
        if (colors != null) {
            for (Map.Entry<String, JsonElement> e : colors.entrySet()) {
                t.colors.put(e.getKey(), parseColor(e.getValue().getAsString()));
            }
        }
        JsonObject sp = Json.obj(o, "sprites");
        if (sp != null) {
            for (Map.Entry<String, JsonElement> e : sp.entrySet()) {
                if (e.getValue().isJsonObject()) {
                    t.sprites.put(e.getKey(), Sprite.fromJson(e.getValue().getAsJsonObject()));
                }
            }
        }
        JsonObject so = Json.obj(o, "sounds");
        if (so != null) {
            for (Map.Entry<String, JsonElement> e : so.entrySet()) {
                t.sounds.put(e.getKey(), e.getValue().getAsString());
            }
        }
        return t;
    }

    /** {@code #RRGGBB} (opaque) or {@code #AARRGGBB}. */
    public static int parseColor(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s;
        try {
            long v = Long.parseLong(h, 16);
            return h.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
        } catch (NumberFormatException e) {
            return 0xFFFF00FF;
        }
    }

    // ---- drawing ----

    /** Draws a themed element (sprite when the theme has one, else the fallback fill). */
    public void draw(Gfx g, String element, int x, int y, int w, int h) {
        Sprite s = sprites.get(element);
        if (s != null) {
            drawSprite(g, s, x, y, w, h);
            return;
        }
        switch (element) {
            case "panel":
                g.fill(x, y, w, h, color("panel_edge"));
                g.fill(x + 1, y + 1, w - 2, h - 2, color("panel"));
                break;
            case "panel_inset":
                g.fill(x, y, w, h, color("panel_inset"));
                g.outline(x, y, w, h, color("panel_edge"));
                break;
            case "header":
                g.fill(x, y, w, h, color("header"));
                break;
            default:
                if (element.startsWith("node_")) {
                    drawNode(g, element, x, y, w, h);
                } else {
                    int c = colors.containsKey(element) || fallback.colors.containsKey(element) ? color(element) : color("button");
                    g.fill(x, y, w, h, c);
                    g.outline(x, y, w, h, color("panel_edge"));
                }
        }
    }

    private void drawNode(Gfx g, String element, int x, int y, int w, int h) {
        int c = color(element);
        g.fill(x, y, w, h, 0xFF101015);
        g.fill(x + 1, y + 1, w - 2, h - 2, c);
        g.fill(x + 2, y + 2, w - 4, h - 4, (c & 0x00FFFFFF) | 0xFF000000);
        g.fill(x + 3, y + 3, w - 6, h - 6, darken(c));
    }

    private static int darken(int c) {
        int r = ((c >> 16) & 0xFF) * 3 / 5;
        int gr = ((c >> 8) & 0xFF) * 3 / 5;
        int b = (c & 0xFF) * 3 / 5;
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private void drawSprite(Gfx g, Sprite s, int x, int y, int w, int h) {
        int l = s.left;
        int t = s.top;
        int r = s.right;
        int b = s.bottom;
        if (l + t + r + b == 0 || w < l + r || h < t + b) {
            g.sprite(s.texture, x, y, w, h, s.u, s.v, s.w, s.h, s.texW, s.texH);
            return;
        }
        int cw = w - l - r;
        int ch = h - t - b;
        int sw = s.w - l - r;
        int sh = s.h - t - b;
        // corners
        g.sprite(s.texture, x, y, l, t, s.u, s.v, l, t, s.texW, s.texH);
        g.sprite(s.texture, x + w - r, y, r, t, s.u + s.w - r, s.v, r, t, s.texW, s.texH);
        g.sprite(s.texture, x, y + h - b, l, b, s.u, s.v + s.h - b, l, b, s.texW, s.texH);
        g.sprite(s.texture, x + w - r, y + h - b, r, b, s.u + s.w - r, s.v + s.h - b, r, b, s.texW, s.texH);
        // edges
        g.sprite(s.texture, x + l, y, cw, t, s.u + l, s.v, sw, t, s.texW, s.texH);
        g.sprite(s.texture, x + l, y + h - b, cw, b, s.u + l, s.v + s.h - b, sw, b, s.texW, s.texH);
        g.sprite(s.texture, x, y + t, l, ch, s.u, s.v + t, l, sh, s.texW, s.texH);
        g.sprite(s.texture, x + w - r, y + t, r, ch, s.u + s.w - r, s.v + t, r, sh, s.texW, s.texH);
        // center
        g.sprite(s.texture, x + l, y + t, cw, ch, s.u + l, s.v + t, sw, sh, s.texW, s.texH);
    }
}

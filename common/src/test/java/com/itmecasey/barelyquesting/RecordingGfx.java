package com.itmecasey.barelyquesting;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.ui.Gfx;

import java.util.ArrayList;
import java.util.List;

/** A Gfx that draws nothing and remembers what it was asked to draw, so tests can find buttons and texts. */
public class RecordingGfx implements Gfx {
    public static final class Text {
        public final String s;
        public final int x;
        public final int y;

        Text(String s, int x, int y) {
            this.s = s;
            this.x = x;
            this.y = y;
        }
    }

    public final int w;
    public final int h;
    public final List<Text> texts = new ArrayList<>();
    public final List<ItemSpec> items = new ArrayList<>();
    public final List<String> tooltips = new ArrayList<>();
    public final List<String> sounds = new ArrayList<>();
    public int fills;
    public int sprites;
    public String clipboard = "";
    private int clipDepth;

    public RecordingGfx(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public void reset() {
        texts.clear();
        items.clear();
        tooltips.clear();
        sounds.clear();
        fills = 0;
        sprites = 0;
    }

    public Text find(String contains) {
        for (Text t : texts) {
            if (t.s.contains(contains)) {
                return t;
            }
        }
        return null;
    }

    public boolean has(String contains) {
        return find(contains) != null;
    }

    @Override public int width() { return w; }
    @Override public int height() { return h; }
    @Override public void fill(int x, int y, int w, int h, int argb) { fills++; }
    @Override public void text(String s, int x, int y, int argb, int style) { texts.add(new Text(s, x, y)); }
    @Override public int textWidth(String s, int style) { return s.length() * 6; }
    @Override public int lineHeight() { return 9; }
    @Override public void item(ItemSpec item, int x, int y, int size) { items.add(item); }
    @Override public void sprite(String texture, int x, int y, int w, int h, int u, int v, int uw, int vh, int texW, int texH) { sprites++; }
    @Override public void clipPush(int x, int y, int w, int h) { clipDepth++; }
    @Override public void clipPop() { clipDepth--; if (clipDepth < 0) throw new IllegalStateException("clip underflow"); }
    @Override public void tooltip(List<String> lines, int mouseX, int mouseY) { tooltips.addAll(lines); }
    @Override public void itemTooltip(ItemSpec item, int mouseX, int mouseY) { tooltips.add(item.id()); }
    @Override public String itemName(ItemSpec item) { return item.id(); }
    @Override public String clipboard() { return clipboard; }
    @Override public void setClipboard(String text) { clipboard = text; }
    @Override public void sound(String soundId, float pitch) { sounds.add(soundId); }

    public int clipDepth() {
        return clipDepth;
    }
}

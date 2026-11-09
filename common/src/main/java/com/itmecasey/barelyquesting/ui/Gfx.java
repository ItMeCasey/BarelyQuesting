package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.core.model.ItemSpec;

import java.util.List;

/**
 * Everything the book needs to draw, with no Minecraft types. {@code GfxImpl} in each version module maps this onto the game's
 * {@code GuiGraphics} (or {@code GuiGraphicsExtractor}); tests use a recording implementation.
 */
public interface Gfx {
    int BOLD = 1;
    int ITALIC = 2;
    int SHADOW = 4;
    int UNDERLINE = 8;
    int STRIKE = 16;

    /** Width and height of the drawing area in GUI pixels. */
    int width();

    int height();

    /** Fills a rectangle with an ARGB color. */
    void fill(int x, int y, int w, int h, int argb);

    void text(String s, int x, int y, int argb, int style);

    int textWidth(String s, int style);

    int lineHeight();

    /** Draws an item icon (and its count when above one) in a size x size box. */
    void item(ItemSpec item, int x, int y, int size);

    /** Draws part of a texture ({@code namespace:path.png}) stretched to w x h. */
    void sprite(String texture, int x, int y, int w, int h, int u, int v, int uw, int vh, int texW, int texH);

    /** Limits drawing to a rectangle until {@link #clipPop()}; clips nest. */
    void clipPush(int x, int y, int w, int h);

    void clipPop();

    /** A tooltip drawn above everything at the end of the frame. */
    void tooltip(List<String> lines, int mouseX, int mouseY);

    /** The game's own item tooltip (name, enchantments, mod name). */
    void itemTooltip(ItemSpec item, int mouseX, int mouseY);

    /** The item's display name, or the id when unknown. */
    String itemName(ItemSpec item);

    String clipboard();

    void setClipboard(String text);

    /** Plays a UI sound by registry id, e.g. {@code minecraft:ui.button.click}. */
    void sound(String soundId, float pitch);

    /** Lifts everything drawn after this call above what was drawn before (versions that order by depth need it). */
    default void raise() {
    }

    /** Selects the font for following text; an empty id means the default font. */
    default void font(String fontId) {
    }

    /** Draws text centered on x. */
    default void centeredText(String s, int cx, int y, int argb, int style) {
        text(s, cx - textWidth(s, style) / 2, y, argb, style);
    }

    default void outline(int x, int y, int w, int h, int argb) {
        fill(x, y, w, 1, argb);
        fill(x, y + h - 1, w, 1, argb);
        fill(x, y, 1, h, argb);
        fill(x + w - 1, y, 1, h, argb);
    }
}

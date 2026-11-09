package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.ui.Gfx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

/**
 * Minecraft 1.20.1: draws the book through {@code GuiGraphics}. Draw order here is by depth, so {@link #raise()} lifts what follows
 * above earlier items, and tooltips are queued and drawn last by {@link #flush()}.
 */
public final class GfxImpl implements Gfx {
    private static final Map<ItemSpec, ItemStack> STACKS = new HashMap<>();
    private static final Map<String, Style> FONTS = new HashMap<>();

    private final GuiGraphics g;
    private final Minecraft mc = Minecraft.getInstance();
    private Style font = Style.EMPTY;
    private final List<Runnable> tooltips = new ArrayList<>();

    public GfxImpl(GuiGraphics graphics) {
        this.g = graphics;
    }

    public static void clearCaches() {
        STACKS.clear();
    }

    private ItemStack stack(ItemSpec spec) {
        if (spec.isTag()) {
            return Mc.stack(spec, mc.level == null ? null : mc.level.registryAccess(), System.currentTimeMillis());
        }
        return STACKS.computeIfAbsent(spec, s -> Mc.stack(s, mc.level == null ? null : mc.level.registryAccess(), 0));
    }

    private Component component(String s, int style) {
        MutableComponent c = Component.literal(s);
        Style st = font;
        if ((style & BOLD) != 0) {
            st = st.withBold(true);
        }
        if ((style & ITALIC) != 0) {
            st = st.withItalic(true);
        }
        if ((style & UNDERLINE) != 0) {
            st = st.withUnderlined(true);
        }
        if ((style & STRIKE) != 0) {
            st = st.withStrikethrough(true);
        }
        return c.withStyle(st);
    }

    private static ItemStack copyOf(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    @Override
    public int width() {
        return g.guiWidth();
    }

    @Override
    public int height() {
        return g.guiHeight();
    }

    @Override
    public void fill(int x, int y, int w, int h, int argb) {
        if (w > 0 && h > 0) {
            g.fill(x, y, x + w, y + h, argb);
        }
    }

    @Override
    public void text(String s, int x, int y, int argb, int style) {
        g.drawString(mc.font, component(s, style), x, y, argb, (style & SHADOW) != 0);
    }

    @Override
    public int textWidth(String s, int style) {
        return mc.font.width(component(s, style));
    }

    @Override
    public int lineHeight() {
        return mc.font.lineHeight;
    }

    @Override
    public void item(ItemSpec item, int x, int y, int size) {
        ItemStack stack = stack(item);
        if (stack.isEmpty()) {
            return;
        }
        if (size == 16) {
            g.renderItem(stack, x, y);
            if (item.count() > 1) {
                g.renderItemDecorations(mc.font, copyOf(stack, item.count()), x, y);
            }
            return;
        }
        float scale = size / 16f;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    @Override
    public void sprite(String texture, int x, int y, int w, int h, int u, int v, int uw, int vh, int texW, int texH) {
        g.blit(Mc.id(texture), x, y, w, h, (float) u, (float) v, uw, vh, texW, texH);
    }

    @Override
    public void clipPush(int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
    }

    @Override
    public void clipPop() {
        g.disableScissor();
    }

    @Override
    public void raise() {
        g.pose().translate(0, 0, 300);
    }

    @Override
    public void tooltip(List<String> lines, int mouseX, int mouseY) {
        List<Component> comps = new ArrayList<>();
        for (String l : lines) {
            comps.add(Component.literal(l));
        }
        tooltips.add(() -> g.renderTooltip(mc.font, comps, Optional.empty(), mouseX, mouseY));
    }

    @Override
    public void itemTooltip(ItemSpec item, int mouseX, int mouseY) {
        ItemStack stack = stack(item);
        if (!stack.isEmpty()) {
            tooltips.add(() -> g.renderTooltip(mc.font, stack, mouseX, mouseY));
        }
    }

    /** Draws the queued tooltips above everything. Call once at the end of the frame. */
    public void flush() {
        if (tooltips.isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 600);
        for (Runnable r : tooltips) {
            r.run();
        }
        g.pose().popPose();
        tooltips.clear();
    }

    @Override
    public String itemName(ItemSpec item) {
        ItemStack stack = stack(item);
        return stack.isEmpty() ? item.id() : stack.getHoverName().getString();
    }

    @Override
    public String clipboard() {
        return mc.keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        mc.keyboardHandler.setClipboard(text);
    }

    @Override
    public void sound(String soundId, float pitch) {
        if (soundId == null || soundId.isEmpty()) {
            return;
        }
        ResourceLocation id = Mc.id(soundId);
        if (BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(id);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
        }
    }

    @Override
    public void font(String fontId) {
        if (fontId == null || fontId.isEmpty()) {
            font = Style.EMPTY;
        } else {
            font = FONTS.computeIfAbsent(fontId, f -> Style.EMPTY.withFont(Mc.id(f)));
        }
    }
}

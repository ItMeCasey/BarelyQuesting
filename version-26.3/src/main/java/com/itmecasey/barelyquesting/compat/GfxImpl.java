package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.ui.Gfx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

/** Minecraft 26.1.2: draws the book through {@code GuiGraphicsExtractor}. */
public final class GfxImpl implements Gfx {
    private static final Map<ItemSpec, ItemStack> STACKS = new HashMap<>();
    private static final Map<String, Style> FONTS = new HashMap<>();

    private final GuiGraphicsExtractor g;
    private final Minecraft mc = Minecraft.getInstance();
    private Style font = Style.EMPTY;

    public GfxImpl(GuiGraphicsExtractor graphics) {
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
        g.text(mc.font, component(s, style), x, y, argb, (style & SHADOW) != 0);
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
            g.item(stack, x, y);
            if (item.count() > 1) {
                g.itemDecorations(mc.font, stack.copyWithCount(item.count()), x, y);
            }
            return;
        }
        float scale = size / 16f;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    @Override
    public void sprite(String texture, int x, int y, int w, int h, int u, int v, int uw, int vh, int texW, int texH) {
        g.blit(RenderPipelines.GUI_TEXTURED, Mc.id(texture), x, y, (float) u, (float) v, w, h, uw, vh, texW, texH);
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
    public void tooltip(List<String> lines, int mouseX, int mouseY) {
        List<Component> comps = new ArrayList<>();
        for (String l : lines) {
            comps.add(Component.literal(l));
        }
        g.setTooltipForNextFrame(mc.font, comps, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public void itemTooltip(ItemSpec item, int mouseX, int mouseY) {
        ItemStack stack = stack(item);
        if (!stack.isEmpty()) {
            g.setTooltipForNextFrame(mc.font, stack, mouseX, mouseY);
        }
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
        Identifier id = Mc.id(soundId);
        if (BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
        }
    }

    @Override
    public void font(String fontId) {
        if (fontId == null || fontId.isEmpty()) {
            font = Style.EMPTY;
        } else {
            font = FONTS.computeIfAbsent(fontId, f -> Style.EMPTY.withFont(new FontDescription.Resource(Mc.id(f))));
        }
    }
}

package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.ui.BookUi;
import com.itmecasey.barelyquesting.ui.ClientState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Minecraft 26.1.2: a thin screen that hands drawing and input to the shared {@link BookUi}. */
public final class QuestScreen extends Screen {
    private final BookUi ui = new BookUi();
    private GfxImpl gfx;

    public QuestScreen(String openQuest) {
        super(Component.translatable("screen.barelyquesting.title"));
        if (openQuest != null && openQuest.equals("!edit")) {
            ui.editing = ClientState.get().canEdit;
        } else if (openQuest != null && !openQuest.isEmpty()) {
            ui.openOn(openQuest);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        gfx = new GfxImpl(graphics);
        ui.render(gfx, mouseX, mouseY);
        if (ui.consumeCloseRequest()) {
            onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // the book draws its own backdrop
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return ui.mouseClicked((int) event.x(), (int) event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return ui.mouseReleased((int) event.x(), (int) event.y(), event.button()) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return ui.mouseDragged((int) event.x(), (int) event.y(), event.button()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return ui.mouseScrolled((int) x, (int) y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (gfx != null && ui.keyPressed(gfx, event.key(), event.modifiers())) {
            return true;
        }
        if (!ui.typing() && !ui.hasModal() && ClientHooks.openKey().matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (event.codepoint() < 0x10000 && ui.charTyped((char) event.codepoint())) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        ui.closed();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

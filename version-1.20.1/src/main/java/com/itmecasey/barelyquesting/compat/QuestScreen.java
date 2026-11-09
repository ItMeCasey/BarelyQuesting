package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.ui.BookUi;
import com.itmecasey.barelyquesting.ui.ClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minecraft 1.20.1: a thin screen that hands drawing and input to the shared {@link BookUi}. */
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

    public BookUi ui() {
        return ui;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        gfx = new GfxImpl(graphics);
        ui.render(gfx, mouseX, mouseY);
        gfx.flush();
        if (ui.consumeCloseRequest()) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // the book draws its own backdrop
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return ui.mouseClicked((int) x, (int) y, button) || super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return ui.mouseReleased((int) x, (int) y, button) || super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return ui.mouseDragged((int) x, (int) y, button) || super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return ui.mouseScrolled((int) x, (int) y, delta) || super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (gfx != null && ui.keyPressed(gfx, key, modifiers)) {
            return true;
        }
        if (!ui.typing() && !ui.hasModal() && ClientHooks.openKey().matches(key, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (ui.charTyped(c)) {
            return true;
        }
        return super.charTyped(c, modifiers);
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

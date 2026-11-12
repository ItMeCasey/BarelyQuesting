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

    public BookUi ui() {
        return ui;
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

    /** The shared book UI numbers buttons 0 left, 1 right, 2 middle; Minecraft 26.3 numbers them 1, 3 and 2. */
    private static int uiButton(MouseButtonEvent event) {
        int button = event.button();
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            return 0;
        }
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) {
            return 1;
        }
        return button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE ? 2 : button;
    }

    /** Minecraft 26.3 reports keys as SDL scancodes; the shared book UI uses GLFW key codes. Keys the book does not use map to -1. */
    private static int glfwKey(int scancode) {
        switch (scancode) {
            case com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE: return 256;
            case com.mojang.blaze3d.platform.InputConstants.KEY_RETURN: return 257;
            case 88: return 335; // keypad enter
            case com.mojang.blaze3d.platform.InputConstants.KEY_TAB: return 258;
            case com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE: return 259;
            case com.mojang.blaze3d.platform.InputConstants.KEY_INSERT: return 260;
            case com.mojang.blaze3d.platform.InputConstants.KEY_DELETE: return 261;
            case com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT: return 262;
            case com.mojang.blaze3d.platform.InputConstants.KEY_LEFT: return 263;
            case com.mojang.blaze3d.platform.InputConstants.KEY_DOWN: return 264;
            case com.mojang.blaze3d.platform.InputConstants.KEY_UP: return 265;
            case com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP: return 266;
            case com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN: return 267;
            case com.mojang.blaze3d.platform.InputConstants.KEY_HOME: return 268;
            case com.mojang.blaze3d.platform.InputConstants.KEY_END: return 269;
            case com.mojang.blaze3d.platform.InputConstants.KEY_A: return 65;
            case com.mojang.blaze3d.platform.InputConstants.KEY_C: return 67;
            case com.mojang.blaze3d.platform.InputConstants.KEY_V: return 86;
            case com.mojang.blaze3d.platform.InputConstants.KEY_X: return 88;
            case com.mojang.blaze3d.platform.InputConstants.KEY_Y: return 89;
            case com.mojang.blaze3d.platform.InputConstants.KEY_Z: return 90;
            default: return -1;
        }
    }

    /** SDL modifier bits to the GLFW ones the book uses (shift 1, control 2, alt 4). */
    private static int glfwMods(int sdl) {
        return ((sdl & com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT) != 0 ? 1 : 0)
                | ((sdl & com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL) != 0 ? 2 : 0)
                | ((sdl & com.mojang.blaze3d.platform.InputConstants.MOD_ALT) != 0 ? 4 : 0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return ui.mouseClicked((int) event.x(), (int) event.y(), uiButton(event)) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return ui.mouseReleased((int) event.x(), (int) event.y(), uiButton(event)) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return ui.mouseDragged((int) event.x(), (int) event.y(), uiButton(event)) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return ui.mouseScrolled((int) x, (int) y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (gfx != null && ui.keyPressed(gfx, glfwKey(event.key()), glfwMods(event.modifiers()))) {
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

    /** The book's key handling, shared by keyPressed and the development tour. */
    public boolean pressKey(int key, int mods) {
        return gfx != null && ui.keyPressed(gfx, key, mods);
    }

    /** The book's character handling, shared by charTyped and the development tour. */
    public boolean typeChar(char c) {
        return ui.charTyped(c);
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

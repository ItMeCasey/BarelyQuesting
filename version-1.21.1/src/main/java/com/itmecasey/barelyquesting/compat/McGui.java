package com.itmecasey.barelyquesting.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Client only: the open screen. Its home on Minecraft moved between versions, so every version module has its own copy. */
public final class McGui {
    private McGui() {
    }

    public static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    /** Whether the key (a GLFW key code) is held right now, even while a screen is open. */
    public static boolean keyDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
    }
}

package com.itmecasey.barelyquesting.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Client only: the open screen. Its home on Minecraft moved between versions, so every version module has its own copy. */
public final class McGui {
    /** Keys the development tour holds down without touching the OS keyboard; empty in normal play. */
    public static final java.util.Set<Integer> forcedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        return forcedKeys.contains(key) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }
}

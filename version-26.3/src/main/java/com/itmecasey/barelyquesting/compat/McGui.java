package com.itmecasey.barelyquesting.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Client only: the open screen. Its home on Minecraft moved between versions, so every version module has its own copy. */
public final class McGui {
    private McGui() {
    }

    public static Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }
}

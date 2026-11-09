package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.theme.ThemeManager;
import com.itmecasey.barelyquesting.ui.ClientEnv;
import com.itmecasey.barelyquesting.ui.ClientState;
import com.itmecasey.barelyquesting.ui.Hud;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ResourceManager;

/** Minecraft 1.20.1: the client side of the mod, called by the loader's client events. */
public final class ClientHooks {
    /** Holds the key category so the client-only class loads only when the key is made, never on a server. */
    private static final class Holder {
        static final KeyMapping OPEN = new KeyMapping("key.barelyquesting.open", InputConstants.Type.KEYSYM, InputConstants.KEY_J, "key.category.barelyquesting.keys");
    }

    private ClientHooks() {
    }

    public static KeyMapping openKey() {
        return Holder.OPEN;
    }

    /** Installs the environment and loads the player's settings. Called once on the client. */
    public static void init() {
        ClientEnv.Holder.set(new McEnv());
        ClientState.get().loadSettings();
        DevHooks.clientInit();
    }

    public static void resourcesReloaded(ResourceManager manager) {
        McEnv.setResources(manager);
        GfxImpl.clearCaches();
        ThemeManager.reload();
        ThemeManager.select(ThemeManager.selectedId());
        ClientState.get().loadSettings();
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        ItemQuestKey.tick();
        while (Holder.OPEN.consumeClick()) {
            if (mc.player != null && McGui.screen() == null) {
                openBook(null);
            }
        }
        String pending = ClientState.get().pendingOpen;
        if (pending != null) {
            ClientState.get().pendingOpen = null;
            if (mc.player != null) {
                openBook(pending);
            }
        }
    }

    public static void openBook(String quest) {
        Minecraft mc = Minecraft.getInstance();
        ClientState st = ClientState.get();
        if (!st.connected) {
            st.hello();
        }
        ClientState.get().playSound("open");
        McGui.setScreen(new QuestScreen(quest));
    }

    public static void hud(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (McGui.screen() instanceof QuestScreen) {
            return;
        }
        Hud.render(new GfxImpl(graphics));
    }

    public static void loggedOut() {
        ClientState.get().reset();
    }

    public static void loggedIn() {
        ClientState.get().loadSettings();
    }
}

package com.itmecasey.barelyquesting.neoforge;

import com.itmecasey.barelyquesting.compat.ClientHooks;
import com.itmecasey.barelyquesting.compat.Mc;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only wiring, in its own class so a dedicated server never loads the client types it names. */
final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {
    }

    /** The in-game config screen (Mods, Barely Questing, Config) NeoForge builds from the specs. */
    static void configScreen(net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                net.neoforged.neoforge.client.gui.ConfigurationScreen::new);
    }

    static void install(IEventBus modBus) {
        Mc.toServer = data -> PacketDistributor.sendToServer(new QuestPayload(data));
        ClientHooks.init();
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(ClientHooks.openKey()));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(Mc.id("barelyquesting", "hud"), (graphics, delta) -> ClientHooks.hud(graphics)));
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                ClientHooks.resourcesReloaded(manager);
            }
        }));
    }
}

/** Game-bus client events. */
@EventBusSubscriber(modid = "barelyquesting", value = Dist.CLIENT)
final class NeoForgeClientEvents {
    private NeoForgeClientEvents() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientHooks.clientTick();
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHooks.loggedOut();
    }

    @SubscribeEvent
    public static void loggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientHooks.loggedIn();
    }
}

package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.ClientHooks;
import com.itmecasey.barelyquesting.compat.Mc;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

/** Client-only wiring, in its own class so a dedicated server never loads the client types it names. */
final class ClientSetup {
    private ClientSetup() {
    }

    static void install(FMLJavaModLoadingContext context) {
        Mc.toServer = data -> ForgeBarelyQuesting.channel.send(new QuestMessage(data), PacketDistributor.SERVER.noArg());
        ClientHooks.init();
        IEventBus modBus = context.getModEventBus();
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(ClientHooks.openKey()));
        modBus.addListener((AddGuiOverlayLayersEvent e) ->
                e.getLayeredDraw().add(Mc.id("barelyquesting", "hud"), (graphics, delta) -> ClientHooks.hud(graphics)));
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                ClientHooks.resourcesReloaded(manager);
            }
        }));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                ClientHooks.clientTick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientHooks.loggedOut());
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> ClientHooks.loggedIn());
    }
}

package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.ClientHooks;
import com.itmecasey.barelyquesting.compat.Mc;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

/** Client-only wiring, in its own class so a dedicated server never loads the client types it names. */
final class ClientSetup {
    private ClientSetup() {
    }

    static void install() {
        Mc.toServer = data -> ForgeBarelyQuesting.channel.send(new QuestMessage(data), PacketDistributor.SERVER.noArg());
        ClientHooks.init();
        RegisterKeyMappingsEvent.BUS.addListener((RegisterKeyMappingsEvent e) -> e.register(ClientHooks.openKey()));
        AddGuiOverlayLayersEvent.BUS.addListener((AddGuiOverlayLayersEvent e) ->
                e.getLayeredDraw().add(Mc.id("barelyquesting", "hud"), (graphics, delta) -> ClientHooks.hud(graphics)));
        RegisterClientReloadListenersEvent.BUS.addListener((RegisterClientReloadListenersEvent e) ->
                e.registerReloadListener(new ResourceManagerReloadListener() {
                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        ClientHooks.resourcesReloaded(manager);
                    }
                }));
        TickEvent.ClientTickEvent.Post.BUS.addListener((TickEvent.ClientTickEvent.Post e) -> ClientHooks.clientTick());
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientHooks.loggedOut());
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> ClientHooks.loggedIn());
    }
}

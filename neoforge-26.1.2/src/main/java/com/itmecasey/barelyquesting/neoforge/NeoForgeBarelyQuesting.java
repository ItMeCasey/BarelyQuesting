package com.itmecasey.barelyquesting.neoforge;

import com.itmecasey.barelyquesting.compat.Mc;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** NeoForge entry point. Everything shared lives in common/ and version-26.1.2/; this class only wires NeoForge into it. */
@Mod("barelyquesting")
public final class NeoForgeBarelyQuesting {
    public NeoForgeBarelyQuesting(IEventBus modBus) {
        Mc.configDir = FMLPaths.CONFIGDIR.get();
        Mc.toPlayer = (ServerPlayer player, byte[] message) -> PacketDistributor.sendToPlayer(player, new QuestPayload(message));
        modBus.addListener(NeoForgeBarelyQuesting::registerPayloads);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientSetup.install(modBus);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(QuestPayload.TYPE, QuestPayload.CODEC, QuestPayload::handle, QuestPayload::handle);
    }
}

package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.Mc;
import com.itmecasey.barelyquesting.core.script.Bridges;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge and NeoForge entry point for Minecraft 1.20.1. NeoForge 1.20.1 keeps the Forge packages, so this one class serves both loaders.
 * Everything shared lives in common/ and version-1.20.1/; this class only wires the loader into it.
 */
@Mod("barelyquesting")
public final class ForgeBarelyQuesting {
    private static final String PROTOCOL = "1";
    static SimpleChannel channel;

    public ForgeBarelyQuesting() {
        Mc.configDir = FMLPaths.CONFIGDIR.get();
        Bridges.install(FMLPaths.GAMEDIR.get());
        channel = NetworkRegistry.ChannelBuilder.named(Mc.id("barelyquesting", "main"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(version -> true)
                .serverAcceptedVersions(version -> true)
                .simpleChannel();
        channel.registerMessage(0, QuestMessage.class, QuestMessage::encode, QuestMessage::decode, QuestMessage::handle);
        Mc.toPlayer = (ServerPlayer player, byte[] message) -> channel.send(PacketDistributor.PLAYER.with(() -> player), new QuestMessage(message));
        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.install();
        }
    }
}

package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.Mc;
import com.itmecasey.barelyquesting.core.script.Bridges;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Forge entry point (1.21.1). Everything shared lives in common/ and version-1.21.1/; this class wires Forge into it. */
@Mod("barelyquesting")
public final class ForgeBarelyQuesting {
    static SimpleChannel channel;

    public ForgeBarelyQuesting(FMLJavaModLoadingContext context) {
        Mc.configDir = FMLPaths.CONFIGDIR.get();
        Bridges.install(FMLPaths.GAMEDIR.get());
        channel = ChannelBuilder.named(Mc.id("barelyquesting", "main"))
                .networkProtocolVersion(1)
                .optional()
                .simpleChannel();
        channel.messageBuilder(QuestMessage.class)
                .codec(QuestMessage.CODEC)
                .consumerMainThread(QuestMessage::handle)
                .add();
        Mc.toPlayer = (ServerPlayer player, byte[] message) -> channel.send(new QuestMessage(message), PacketDistributor.PLAYER.with(player));
        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.install(context);
        }
    }
}

package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.DevHooks;
import com.itmecasey.barelyquesting.compat.Mc;
import com.itmecasey.barelyquesting.compat.QuestCommands;
import com.itmecasey.barelyquesting.compat.ServerHooks;
import com.itmecasey.barelyquesting.core.script.Bridges;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Forge entry point (26.1.2). Everything shared lives in common/ and version-26.1.2/; this class wires Forge into it. */
@Mod("barelyquesting")
public final class ForgeBarelyQuesting {
    static SimpleChannel channel;

    public ForgeBarelyQuesting(FMLJavaModLoadingContext context) {
        QuestItems.register(context.getModBusGroup());
        Mc.configDir = FMLPaths.CONFIGDIR.get();
        QuestConfig.register(context);
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

        RegisterCommandsEvent.BUS.addListener((RegisterCommandsEvent e) -> QuestCommands.register(e.getDispatcher()));
        ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> {
            ServerHooks.started(e.getServer());
            DevHooks.serverStarted(e.getServer());
        });
        ServerStoppingEvent.BUS.addListener((ServerStoppingEvent e) -> ServerHooks.stopping());
        TickEvent.ServerTickEvent.Post.BUS.addListener((TickEvent.ServerTickEvent.Post e) -> ServerHooks.tick(e.server()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerHooks.join(player);
            }
        });
        LivingDeathEvent.BUS.addListener((LivingDeathEvent e) -> {
            Entity source = e.getSource().getEntity();
            if (source instanceof ServerPlayer player) {
                ServerHooks.kill(player, e.getEntity());
            }
            return false;
        });
        PlayerEvent.ItemCraftedEvent.BUS.addListener((PlayerEvent.ItemCraftedEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerHooks.crafted(player, e.getCrafting());
            }
        });
        PlayerEvent.ItemSmeltedEvent.BUS.addListener((PlayerEvent.ItemSmeltedEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerHooks.crafted(player, e.getSmelting());
            }
        });
        PlayerEvent.ItemPickupEvent.BUS.addListener((PlayerEvent.ItemPickupEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerHooks.pickedUp(player, e.getStack());
            }
        });
        AdvancementEvent.AdvancementEarnEvent.BUS.addListener((AdvancementEvent.AdvancementEarnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerHooks.advancement(player, e.getAdvancement().id().toString());
            }
        });

        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.install();
        }
    }
}

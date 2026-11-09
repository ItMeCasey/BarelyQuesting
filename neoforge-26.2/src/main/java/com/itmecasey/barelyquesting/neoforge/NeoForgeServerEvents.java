package com.itmecasey.barelyquesting.neoforge;

import com.itmecasey.barelyquesting.compat.DevHooks;
import com.itmecasey.barelyquesting.compat.QuestCommands;
import com.itmecasey.barelyquesting.compat.ServerHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-side game events, forwarded to the shared hooks. */
@EventBusSubscriber(modid = "barelyquesting")
public final class NeoForgeServerEvents {
    private NeoForgeServerEvents() {
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        QuestCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        ServerHooks.started(event.getServer());
        DevHooks.serverStarted(event.getServer());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        ServerHooks.stopping();
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        ServerHooks.tick(event.getServer());
    }

    @SubscribeEvent
    public static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerHooks.join(player);
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer player) {
            ServerHooks.kill(player, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerHooks.crafted(player, event.getCrafting());
        }
    }

    @SubscribeEvent
    public static void smelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerHooks.crafted(player, event.getSmelting());
        }
    }

    @SubscribeEvent
    public static void pickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ServerHooks.pickedUp(player, event.getOriginalStack());
        }
    }

    @SubscribeEvent
    public static void advancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerHooks.advancement(player, event.getAdvancement().id().toString());
        }
    }
}

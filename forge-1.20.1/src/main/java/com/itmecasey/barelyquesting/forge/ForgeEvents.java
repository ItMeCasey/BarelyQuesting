package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.DevHooks;
import com.itmecasey.barelyquesting.compat.QuestCommands;
import com.itmecasey.barelyquesting.compat.ServerHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side game events, forwarded to the shared hooks. */
@Mod.EventBusSubscriber(modid = "barelyquesting")
public final class ForgeEvents {
    private ForgeEvents() {
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
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerHooks.tick(event.getServer());
        }
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
    public static void pickup(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerHooks.pickedUp(player, event.getStack());
        }
    }

    @SubscribeEvent
    public static void advancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerHooks.advancement(player, event.getAdvancement().getId().toString());
        }
    }
}

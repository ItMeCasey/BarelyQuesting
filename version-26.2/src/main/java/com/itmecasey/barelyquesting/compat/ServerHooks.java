package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.script.QuestApi;
import com.itmecasey.barelyquesting.core.task.TaskEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/** The loaders call these from their events. Everything here runs on the server thread. */
public final class ServerHooks {
    private static final Logger LOG = Logger.getLogger("barelyquesting");
    private static QuestServer quests;

    private ServerHooks() {
    }

    public static QuestServer quests() {
        return quests;
    }

    /** True when a world is loaded and quests are running. */
    public static boolean running() {
        return quests != null;
    }

    public static void started(MinecraftServer server) {
        Mc.server = server;
        Mc.installMatchers();
        Path world = server.getWorldPath(LevelResource.ROOT).resolve("barelyquesting");
        quests = new QuestServer(Mc.configDir.resolve("barelyquesting"), world, (uuid, bytes) -> {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                Mc.toPlayer.send(p, bytes);
            }
        }, new QuestEngine.Host() {
            @Override
            public PlayerView online(UUID id) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                return p == null ? null : new McPlayer(p);
            }

            @Override
            public long nowMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public List<PlayerView> players() {
                List<PlayerView> out = new ArrayList<>();
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    out.add(new McPlayer(p));
                }
                return out;
            }
        });
        quests.start();
        QuestApi.attach(quests);
        LOG.info("Barely Questing: " + quests.engine().book().allQuests().size() + " quests in "
                + quests.engine().book().chapters.size() + " chapters");
    }

    public static void stopping() {
        if (quests != null) {
            QuestApi.detach();
            quests.stop();
            quests = null;
        }
        Mc.server = null;
    }

    public static void tick(MinecraftServer server) {
        if (quests != null) {
            quests.tick(server.getTickCount());
        }
    }

    public static void join(ServerPlayer player) {
        if (quests != null) {
            quests.onJoin(new McPlayer(player));
        }
    }

    public static void message(ServerPlayer player, byte[] message) {
        if (quests != null) {
            quests.handle(new McPlayer(player), message);
        }
    }

    public static void kill(ServerPlayer player, Entity victim) {
        if (quests != null) {
            quests.engine().event(TaskEvent.kill(new McPlayer(player), Mc.entityTypeId(victim)));
        }
    }

    public static void crafted(ServerPlayer player, ItemStack stack) {
        if (quests != null && !stack.isEmpty()) {
            quests.engine().event(TaskEvent.craft(new McPlayer(player), spec(player, stack)));
        }
    }

    public static void pickedUp(ServerPlayer player, ItemStack stack) {
        if (quests != null && !stack.isEmpty()) {
            quests.engine().event(TaskEvent.pickup(new McPlayer(player), spec(player, stack)));
        }
    }

    public static void advancement(ServerPlayer player, String id) {
        if (quests != null) {
            quests.engine().event(TaskEvent.advancement(new McPlayer(player), id));
        }
    }

    private static ItemSpec spec(ServerPlayer player, ItemStack stack) {
        return Mc.specOf(stack, player.level().registryAccess());
    }
}

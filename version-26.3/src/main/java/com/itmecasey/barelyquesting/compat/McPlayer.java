package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import java.util.Map;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;

/** The engine's view of a real player on a running server. */
public final class McPlayer implements PlayerView {
    private final ServerPlayer player;

    public McPlayer(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer player() {
        return player;
    }

    private ServerLevel level() {
        return player.level();
    }

    @Override
    public UUID uuid() {
        return player.getUUID();
    }

    @Override
    public String name() {
        return player.getName().getString();
    }

    @Override
    public int countItems(ItemSpec spec) {
        Inventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (Mc.matches(spec, s, level().registryAccess())) {
                n += s.getCount();
            }
        }
        return n;
    }

    @Override
    public int takeItems(ItemSpec spec, int max) {
        Inventory inv = player.getInventory();
        int left = max;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (Mc.matches(spec, s, level().registryAccess())) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        if (left < max && player.connection != null) {
            player.containerMenu.broadcastChanges();
        }
        return max - left;
    }

    @Override
    public void giveItem(ItemSpec spec) {
        ItemStack stack = Mc.stack(spec, level().registryAccess(), 0);
        int total = Math.max(1, spec.count());
        while (total > 0 && !stack.isEmpty()) {
            int n = Math.min(total, stack.getMaxStackSize());
            ItemStack part = stack.copyWithCount(n);
            if (player.connection != null) {
                player.getInventory().placeItemBackInInventory(part, net.minecraft.util.Prediction.SERVER_ONLY);
            } else {
                player.getInventory().add(part);
                if (!part.isEmpty()) {
                    player.drop(part, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
            total -= n;
        }
    }

    @Override
    public void addExperiencePoints(int points) {
        player.giveExperiencePoints(points);
    }

    @Override
    public void addExperienceLevels(int levels) {
        player.giveExperienceLevels(levels);
    }

    @Override
    public void runCommand(String command) {
        var server = level().getServer();
        var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    @Override
    public String dimension() {
        return level().dimension().identifier().toString();
    }

    @Override
    public String biome() {
        return level().getBiome(player.blockPosition()).unwrapKey().map(k -> k.identifier().toString()).orElse("");
    }

    @Override
    public double x() {
        return player.getX();
    }

    @Override
    public double y() {
        return player.getY();
    }

    @Override
    public double z() {
        return player.getZ();
    }

    @Override
    public boolean inStructure(String structureId) {
        ServerLevel level = level();
        BlockPos pos = player.blockPosition();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Identifier wanted = Mc.id(structureId);
        for (Map.Entry<Structure, ?> e : level.structureManager().getAllStructuresAt(pos).entrySet()) {
            Identifier key = registry.getKey(e.getKey());
            if (wanted.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAdvancement(String id) {
        AdvancementHolder holder = level().getServer().getAdvancements().get(Mc.id(id));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public long statistic(String type, String key) {
        try {
            Identifier id = Mc.id(key);
            Stat<?> stat;
            switch (type) {
                case "custom":
                    stat = Stats.CUSTOM.get(id);
                    break;
                case "mined":
                    stat = stat(Stats.BLOCK_MINED, BuiltInRegistries.BLOCK.getValue(id));
                    break;
                case "crafted":
                    stat = stat(Stats.ITEM_CRAFTED, BuiltInRegistries.ITEM.getValue(id));
                    break;
                case "used":
                    stat = stat(Stats.ITEM_USED, BuiltInRegistries.ITEM.getValue(id));
                    break;
                case "broken":
                    stat = stat(Stats.ITEM_BROKEN, BuiltInRegistries.ITEM.getValue(id));
                    break;
                case "picked_up":
                    stat = stat(Stats.ITEM_PICKED_UP, BuiltInRegistries.ITEM.getValue(id));
                    break;
                case "dropped":
                    stat = stat(Stats.ITEM_DROPPED, BuiltInRegistries.ITEM.getValue(id));
                    break;
                case "killed":
                    stat = stat(Stats.ENTITY_KILLED, BuiltInRegistries.ENTITY_TYPE.getValue(id));
                    break;
                case "killed_by":
                    stat = stat(Stats.ENTITY_KILLED_BY, BuiltInRegistries.ENTITY_TYPE.getValue(id));
                    break;
                default:
                    return 0;
            }
            return player.getStats().getValue(stat);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static <T> Stat<T> stat(StatType<T> type, T value) {
        return type.get(value);
    }

    @Override
    public void message(String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    @Override
    public boolean isOperator() {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    @Override
    public boolean isCreative() {
        return player.isCreative();
    }
}

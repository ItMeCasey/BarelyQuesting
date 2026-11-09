package com.itmecasey.barelyquesting.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The craftable Quest Book: right-click opens the book, the same as the J key and {@code /barelyquesting open}. */
public final class QuestBookItem extends Item {
    public QuestBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp && ServerHooks.quests() != null) {
            ServerHooks.quests().open(new McPlayer(sp), null);
        }
        return InteractionResult.SUCCESS;
    }
}

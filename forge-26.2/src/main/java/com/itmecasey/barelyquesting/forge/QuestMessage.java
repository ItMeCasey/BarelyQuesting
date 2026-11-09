package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.ServerHooks;
import com.itmecasey.barelyquesting.ui.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** The one message both ways: a byte array in the mod's own wire format (see core.net.Wire). */
public record QuestMessage(byte[] data) {
    static final StreamCodec<FriendlyByteBuf, QuestMessage> CODEC =
            ByteBufCodecs.byteArray(1 << 20).map(QuestMessage::new, QuestMessage::data).cast();

    /** Runs on the main thread. A sender means the message came from a client. */
    static void handle(QuestMessage message, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            ServerHooks.message(player, message.data());
        } else {
            ClientState.get().handle(message.data());
        }
    }
}

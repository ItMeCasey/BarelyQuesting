package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.ServerHooks;
import com.itmecasey.barelyquesting.ui.ClientState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** The one message both ways: a byte array in the mod's own wire format (see core.net.Wire). */
public record QuestMessage(byte[] data) {
    static void encode(QuestMessage message, FriendlyByteBuf buffer) {
        buffer.writeByteArray(message.data());
    }

    static QuestMessage decode(FriendlyByteBuf buffer) {
        return new QuestMessage(buffer.readByteArray(1 << 20));
    }

    static void handle(QuestMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (player != null) {
                ServerHooks.message(player, message.data());
            } else {
                ClientState.get().handle(message.data());
            }
        });
        ctx.setPacketHandled(true);
    }
}

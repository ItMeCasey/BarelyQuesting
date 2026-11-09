package com.itmecasey.barelyquesting.neoforge;

import com.itmecasey.barelyquesting.compat.Mc;
import com.itmecasey.barelyquesting.compat.ServerHooks;
import com.itmecasey.barelyquesting.ui.ClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The one message both ways: a byte array in the mod's own wire format (see core.net.Wire). */
public record QuestPayload(byte[] data) implements CustomPacketPayload {
    public static final Type<QuestPayload> TYPE = new Type<>(Mc.id("barelyquesting", "main"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestPayload> CODEC =
            ByteBufCodecs.byteArray(1 << 20).map(QuestPayload::new, QuestPayload::data).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.SERVERBOUND) {
                if (context.player() instanceof ServerPlayer player) {
                    ServerHooks.message(player, payload.data());
                }
            } else {
                ClientState.get().handle(payload.data());
            }
        });
    }
}

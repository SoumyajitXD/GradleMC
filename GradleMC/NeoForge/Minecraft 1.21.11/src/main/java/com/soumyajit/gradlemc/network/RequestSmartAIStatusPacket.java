package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RequestSmartAIStatusPacket implements CustomPacketPayload {
    public static final RequestSmartAIStatusPacket INSTANCE = new RequestSmartAIStatusPacket();
    public static final CustomPacketPayload.Type<RequestSmartAIStatusPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("gradlemc", "request_smart_ai_status"));
    public static final StreamCodec<FriendlyByteBuf, RequestSmartAIStatusPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private RequestSmartAIStatusPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestSmartAIStatusPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sender) {
            AdaptiveSmartAIManager.sync(sender);
        }
    }
}

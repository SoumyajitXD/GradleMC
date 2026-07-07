package com.soumyajit.gradlemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class RequestSmartAIStatusPacket implements CustomPacketPayload {
    public static final Type<RequestSmartAIStatusPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("gradlemc", "request_smart_ai_status"));
    public static final RequestSmartAIStatusPacket INSTANCE = new RequestSmartAIStatusPacket();
    public static final StreamCodec<FriendlyByteBuf, RequestSmartAIStatusPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private RequestSmartAIStatusPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RequestSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }
}

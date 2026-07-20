package com.soumyajit.gradlemc.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Optional;

public record RequestSmartAIStatusPacket(int protocolVersion, int requestId) {
    private static final int ENCODED_SIZE = Integer.BYTES * 2;

    public static void encode(RequestSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.protocolVersion());
        buffer.writeInt(packet.requestId());
    }

    public static Optional<RequestSmartAIStatusPacket> decode(FriendlyByteBuf buffer) {
        if (buffer == null || buffer.readableBytes() != ENCODED_SIZE) return Optional.empty();
        int protocol = buffer.readInt();
        int requestId = buffer.readInt();
        return protocol > 0 && requestId > 0
                ? Optional.of(new RequestSmartAIStatusPacket(protocol, requestId))
                : Optional.empty();
    }
}

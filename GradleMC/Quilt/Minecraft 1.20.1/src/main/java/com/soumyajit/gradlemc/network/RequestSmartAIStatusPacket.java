package com.soumyajit.gradlemc.network;

import net.minecraft.network.FriendlyByteBuf;

public final class RequestSmartAIStatusPacket {
    public static final RequestSmartAIStatusPacket INSTANCE = new RequestSmartAIStatusPacket();

    private RequestSmartAIStatusPacket() {
    }

    public static void encode(RequestSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }
}

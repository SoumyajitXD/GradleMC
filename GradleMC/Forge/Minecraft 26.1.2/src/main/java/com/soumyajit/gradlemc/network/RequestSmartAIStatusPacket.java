package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public final class RequestSmartAIStatusPacket {
    public static final RequestSmartAIStatusPacket INSTANCE = new RequestSmartAIStatusPacket();

    private RequestSmartAIStatusPacket() {
    }

    public static void encode(RequestSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(RequestSmartAIStatusPacket packet, CustomPayloadEvent.Context context) {
        if (!context.isServerSide()) {
            context.setPacketHandled(true);
            return;
        }
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            AdaptiveSmartAIManager.sync(sender);
        }
        context.setPacketHandled(true);
    }
}

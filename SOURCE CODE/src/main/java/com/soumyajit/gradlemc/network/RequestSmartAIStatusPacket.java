package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RequestSmartAIStatusPacket {
    public static final RequestSmartAIStatusPacket INSTANCE = new RequestSmartAIStatusPacket();

    private RequestSmartAIStatusPacket() {
    }

    public static void encode(RequestSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(RequestSmartAIStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                AdaptiveSmartAIManager.sync(sender);
            }
        });
        context.setPacketHandled(true);
    }
}

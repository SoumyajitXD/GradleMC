package com.soumyajit.gradlemc.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

public final class GradleMCClientNetwork {
    private GradleMCClientNetwork() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(GradleMCNetwork.OPEN_GUI, (client, handler, buffer, responseSender) -> {
            OpenGradleMCGuiPacket packet = OpenGradleMCGuiPacket.decode(buffer);
            client.execute(() -> OpenGradleMCGuiPacket.handle(packet));
        });
        ClientPlayNetworking.registerGlobalReceiver(GradleMCNetwork.SYNC_SMART_AI_STATUS, (client, handler, buffer, responseSender) -> {
            SyncSmartAIStatusPacket packet = SyncSmartAIStatusPacket.decode(buffer);
            client.execute(() -> SyncSmartAIStatusPacket.handle(packet));
        });
    }

    public static void requestSmartAIStatus() {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        RequestSmartAIStatusPacket.encode(RequestSmartAIStatusPacket.INSTANCE, buffer);
        ClientPlayNetworking.send(GradleMCNetwork.REQUEST_SMART_AI_STATUS, buffer);
    }
}

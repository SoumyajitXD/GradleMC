package com.soumyajit.gradlemc.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class GradleMCClientNetwork {
    private GradleMCClientNetwork() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(OpenGradleMCGuiPacket.TYPE, (packet, context) ->
                context.client().execute(() -> OpenGradleMCGuiPacket.handle(packet)));
        ClientPlayNetworking.registerGlobalReceiver(SyncSmartAIStatusPacket.TYPE, (packet, context) ->
                context.client().execute(() -> SyncSmartAIStatusPacket.handle(packet)));
    }

    public static void requestSmartAIStatus() {
        ClientPlayNetworking.send(RequestSmartAIStatusPacket.INSTANCE);
    }
}

package com.soumyajit.gradlemc.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Optional;

public final class GradleMCClientNetwork {
    private static boolean receiversRegistered;
    private GradleMCClientNetwork() {
    }

    public static void registerReceivers() {
        if (receiversRegistered) return;
        ClientPlayNetworking.registerGlobalReceiver(GradleMCNetwork.OPEN_GUI, (client, handler, buffer, responseSender) -> {
            OpenGradleMCGuiPacket packet = OpenGradleMCGuiPacket.decode(buffer);
            client.execute(() -> OpenGradleMCGuiPacket.handle(packet));
        });
        ClientPlayNetworking.registerGlobalReceiver(GradleMCNetwork.SYNC_SMART_AI_STATUS, (client, handler, buffer, responseSender) -> {
            // Decode/copy before leaving the networking callback; the buffer is not retained.
            Optional<SyncSmartAIStatusPacket> packet = SyncSmartAIStatusPacket.decode(buffer);
            packet.ifPresent(value -> client.execute(() -> GradleMCGuiBridge.acceptStatusResponse(value, System.currentTimeMillis())));
        });
        receiversRegistered = true;
    }

    public static void onJoin() {
        GradleMCGuiBridge.beginConnection();
        if (!ClientPlayNetworking.canSend(GradleMCNetwork.REQUEST_SMART_AI_STATUS)) {
            GradleMCGuiBridge.markServerUnsupported("This server does not advertise GradleMC support. Local diagnostics remain available.");
            return;
        }
        requestSmartAIStatus();
    }

    public static void onDisconnect() {
        GradleMCGuiBridge.disconnect("Disconnected from the GradleMC server. Local diagnostics remain available.");
    }

    public static void onClientTick() {
        GradleMCGuiBridge.expireRequests(System.currentTimeMillis());
    }

    public static boolean requestSmartAIStatus() {
        if (!ClientPlayNetworking.canSend(GradleMCNetwork.REQUEST_SMART_AI_STATUS)) {
            GradleMCGuiBridge.markServerUnsupported("This server does not advertise GradleMC support. Local diagnostics remain available.");
            return false;
        }
        Optional<ClientRequestTracker.Request> request = GradleMCGuiBridge.beginStatusRequest(System.currentTimeMillis());
        if (request.isEmpty()) return false;
        FriendlyByteBuf buffer = PacketByteBufs.create();
        RequestSmartAIStatusPacket.encode(new RequestSmartAIStatusPacket(GradleMCNetwork.PROTOCOL_VERSION, request.get().id()), buffer);
        try {
            ClientPlayNetworking.send(GradleMCNetwork.REQUEST_SMART_AI_STATUS, buffer);
            return true;
        } catch (IllegalStateException ignored) {
            GradleMCGuiBridge.cancelRequest(request.get().id());
            GradleMCGuiBridge.markServerUnavailable("The connection closed before GradleMC could refresh server diagnostics.");
            return false;
        }
    }
}

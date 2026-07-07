package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.SmartAIStatus;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GradleMCNetwork {
    private static final String PROTOCOL_VERSION = "4";
    private static boolean registered;

    private GradleMCNetwork() {
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        if (registered) {
            return;
        }
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(OpenGradleMCGuiPacket.TYPE, OpenGradleMCGuiPacket.STREAM_CODEC);
        registrar.playToClient(SyncSmartAIStatusPacket.TYPE, SyncSmartAIStatusPacket.STREAM_CODEC);
        registrar.playToServer(RequestSmartAIStatusPacket.TYPE, RequestSmartAIStatusPacket.STREAM_CODEC, RequestSmartAIStatusPacket::handle);
        registered = true;
    }

    public static void openGui(ServerPlayer player) {
        if (!registered || player == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, OpenGradleMCGuiPacket.INSTANCE);
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status) {
        syncSmartAIStatus(player, status, player == null ? GuiStatusSnapshot.empty() : GuiStatusSnapshot.capture(player.level().getServer()));
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status, GuiStatusSnapshot guiStatus) {
        if (!registered || player == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new SyncSmartAIStatusPacket(status, guiStatus));
    }

    public static void requestSmartAIStatus() {
        if (!registered) {
            return;
        }
        ClientPacketDistributor.sendToServer(RequestSmartAIStatusPacket.INSTANCE);
    }
}

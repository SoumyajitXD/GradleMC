package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class GradleMCNetwork {
    public static final Identifier OPEN_GUI = id("open_gui");
    public static final Identifier SYNC_SMART_AI_STATUS = id("sync_smart_ai_status");
    public static final Identifier REQUEST_SMART_AI_STATUS = id("request_smart_ai_status");
    private static boolean registered;

    private GradleMCNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.clientboundPlay().register(OpenGradleMCGuiPacket.TYPE, OpenGradleMCGuiPacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncSmartAIStatusPacket.TYPE, SyncSmartAIStatusPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestSmartAIStatusPacket.TYPE, RequestSmartAIStatusPacket.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestSmartAIStatusPacket.TYPE, (packet, context) ->
                context.server().execute(() -> AdaptiveSmartAIManager.sync(context.player())));
        registered = true;
    }

    public static void openGui(ServerPlayer player) {
        if (!registered || player == null) {
            return;
        }
        ServerPlayNetworking.send(player, OpenGradleMCGuiPacket.INSTANCE);
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status) {
        syncSmartAIStatus(player, status, player == null ? GuiStatusSnapshot.empty() : GuiStatusSnapshot.capture(player.level().getServer()));
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status, GuiStatusSnapshot guiStatus) {
        if (!registered || player == null) {
            return;
        }
        ServerPlayNetworking.send(player, new SyncSmartAIStatusPacket(status, guiStatus));
    }

    public static void requestSmartAIStatus() {
        // TODO Fabric port: wire GUI refreshes through the client source set.
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GradleMC.MOD_ID, path);
    }
}

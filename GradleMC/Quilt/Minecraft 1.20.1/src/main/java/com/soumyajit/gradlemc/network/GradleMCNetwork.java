package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class GradleMCNetwork {
    public static final ResourceLocation OPEN_GUI = id("open_gui");
    public static final ResourceLocation SYNC_SMART_AI_STATUS = id("sync_smart_ai_status");
    public static final ResourceLocation REQUEST_SMART_AI_STATUS = id("request_smart_ai_status");
    private static boolean registered;

    private GradleMCNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_SMART_AI_STATUS, (server, player, handler, buffer, responseSender) ->
                server.execute(() -> AdaptiveSmartAIManager.sync(player)));
        registered = true;
    }

    public static void openGui(ServerPlayer player) {
        if (!registered || player == null) {
            return;
        }
        FriendlyByteBuf buffer = PacketByteBufs.create();
        OpenGradleMCGuiPacket.encode(OpenGradleMCGuiPacket.INSTANCE, buffer);
        ServerPlayNetworking.send(player, OPEN_GUI, buffer);
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status) {
        syncSmartAIStatus(player, status, player == null ? GuiStatusSnapshot.empty() : GuiStatusSnapshot.capture(player.server));
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status, GuiStatusSnapshot guiStatus) {
        if (!registered || player == null) {
            return;
        }
        FriendlyByteBuf buffer = PacketByteBufs.create();
        SyncSmartAIStatusPacket.encode(new SyncSmartAIStatusPacket(status, guiStatus), buffer);
        ServerPlayNetworking.send(player, SYNC_SMART_AI_STATUS, buffer);
    }

    public static void requestSmartAIStatus() {
        // TODO Fabric port: wire GUI refreshes through the client source set.
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(GradleMC.MOD_ID, path);
    }
}

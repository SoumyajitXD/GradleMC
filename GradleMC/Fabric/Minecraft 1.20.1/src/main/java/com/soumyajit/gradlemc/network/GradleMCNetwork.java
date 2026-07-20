package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

public final class GradleMCNetwork {
    public static final int PROTOCOL_VERSION = 1;
    public static final ResourceLocation OPEN_GUI = id("open_gui");
    public static final ResourceLocation SYNC_SMART_AI_STATUS = id("sync_smart_ai_status");
    public static final ResourceLocation REQUEST_SMART_AI_STATUS = id("request_smart_ai_status");
    private static boolean registered;
    private static final long STATUS_REQUEST_COOLDOWN_MILLIS = 250L;
    private static final Map<UUID, Long> LAST_STATUS_REQUEST_MILLIS = new ConcurrentHashMap<>();

    private GradleMCNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_SMART_AI_STATUS, (server, player, handler, buffer, responseSender) -> {
            // Decode while the packet buffer is valid; only immutable values reach the server thread.
            RequestSmartAIStatusPacket.decode(buffer).ifPresent(packet -> {
                if (tryAcceptStatusRequest(player.getUUID(), System.currentTimeMillis())) server.execute(() -> handleStatusRequest(player, packet));
            });
        });
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
        SyncSmartAIStatusPacket.encode(new SyncSmartAIStatusPacket(PROTOCOL_VERSION, 0, true,
                player.hasPermissions(2), GradleMC.version(), "", status, guiStatus), buffer);
        ServerPlayNetworking.send(player, SYNC_SMART_AI_STATUS, buffer);
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) LAST_STATUS_REQUEST_MILLIS.remove(player.getUUID());
    }

    private static void handleStatusRequest(ServerPlayer player, RequestSmartAIStatusPacket request) {
        if (player == null || player.connection == null) return;
        boolean compatible = request.protocolVersion() == PROTOCOL_VERSION;
        sendStatusResponse(player, request, compatible, compatible && player.hasPermissions(2),
                compatible ? "" : "GradleMC protocol mismatch: client=" + request.protocolVersion() + ", server=" + PROTOCOL_VERSION + ".",
                compatible ? AdaptiveSmartAIManager.statusFor(player) : SmartAIStatus.disabled(),
                compatible ? GuiStatusSnapshot.capture(player.server) : GuiStatusSnapshot.empty());
    }

    static boolean tryAcceptStatusRequest(UUID playerId, long now) {
        if (playerId == null) return false;
        AtomicBoolean accepted = new AtomicBoolean();
        LAST_STATUS_REQUEST_MILLIS.compute(playerId, (ignored, previous) -> {
            if (previous == null || now - previous >= STATUS_REQUEST_COOLDOWN_MILLIS) { accepted.set(true); return now; }
            return previous;
        });
        return accepted.get();
    }

    private static void sendStatusResponse(ServerPlayer player, RequestSmartAIStatusPacket request,
                                           boolean serverDiagnosticsAllowed, boolean administrativeDiagnosticsAllowed,
                                           String statusMessage, SmartAIStatus status, GuiStatusSnapshot guiStatus) {
        FriendlyByteBuf response = PacketByteBufs.create();
        SyncSmartAIStatusPacket.encode(new SyncSmartAIStatusPacket(PROTOCOL_VERSION, request.requestId(), serverDiagnosticsAllowed,
                administrativeDiagnosticsAllowed, GradleMC.version(), statusMessage, status, guiStatus), response);
        ServerPlayNetworking.send(player, SYNC_SMART_AI_STATUS, response);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(GradleMC.MOD_ID, path);
    }
}

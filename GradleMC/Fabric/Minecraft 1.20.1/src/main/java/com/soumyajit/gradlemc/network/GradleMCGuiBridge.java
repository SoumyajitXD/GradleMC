package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.SmartAIStatus;

public final class GradleMCGuiBridge {
    private static volatile Runnable clientOpener;
    private static volatile SmartAIStatus latestSmartAIStatus = SmartAIStatus.disabled();
    private static volatile GuiStatusSnapshot latestGuiStatus = GuiStatusSnapshot.empty();
    private static volatile long latestSmartAIStatusUpdatedAtMillis;
    private static final ClientRequestTracker REQUESTS = new ClientRequestTracker();
    private static volatile ServerCapabilityState serverCapabilities = ServerCapabilityState.localOnly(0L, "No server connection.");

    private GradleMCGuiBridge() {
    }

    public static void registerClientOpener(Runnable opener) {
        clientOpener = opener;
    }

    public static void open() {
        if (clientOpener != null) {
            clientOpener.run();
        }
    }

    public static void updateSmartAIStatus(SmartAIStatus status) {
        updateStatus(status, latestGuiStatus);
    }

    public static void updateStatus(SmartAIStatus status, GuiStatusSnapshot guiStatus) {
        latestSmartAIStatus = status == null ? SmartAIStatus.disabled() : status;
        latestGuiStatus = guiStatus == null ? GuiStatusSnapshot.empty() : guiStatus;
        latestSmartAIStatusUpdatedAtMillis = System.currentTimeMillis();
    }

    public static long beginConnection() {
        long generation = REQUESTS.beginConnection();
        latestSmartAIStatus = SmartAIStatus.disabled();
        latestGuiStatus = GuiStatusSnapshot.empty();
        latestSmartAIStatusUpdatedAtMillis = 0L;
        serverCapabilities = ServerCapabilityState.negotiating(generation);
        return generation;
    }

    public static void markServerUnsupported(String reason) {
        long generation = REQUESTS.generation();
        serverCapabilities = new ServerCapabilityState(ServerCapabilityState.Availability.UNSUPPORTED, 0,
                false, false, "", reason, generation);
    }

    public static void markServerUnavailable(String reason) {
        long generation = REQUESTS.generation();
        serverCapabilities = ServerCapabilityState.localOnly(generation, reason);
    }

    public static void disconnect(String reason) {
        long generation = REQUESTS.beginConnection();
        latestSmartAIStatus = SmartAIStatus.disabled();
        latestGuiStatus = GuiStatusSnapshot.empty();
        latestSmartAIStatusUpdatedAtMillis = 0L;
        serverCapabilities = new ServerCapabilityState(ServerCapabilityState.Availability.DISCONNECTED, 0,
                false, false, "", reason, generation);
    }

    public static java.util.Optional<ClientRequestTracker.Request> beginStatusRequest(long nowMillis) {
        return REQUESTS.begin("status", nowMillis);
    }

    public static void cancelRequest(int requestId) {
        REQUESTS.cancel(requestId);
    }

    public static void expireRequests(long nowMillis) {
        if (REQUESTS.expire(nowMillis) <= 0) return;
        long generation = REQUESTS.generation();
        ServerCapabilityState current = serverCapabilities;
        if (current.connectionGeneration() == generation && current.availability() == ServerCapabilityState.Availability.NEGOTIATING) {
            serverCapabilities = ServerCapabilityState.localOnly(generation,
                    "The GradleMC server did not respond in time. Local diagnostics remain available.");
        }
    }

    public static boolean acceptStatusResponse(SyncSmartAIStatusPacket packet, long nowMillis) {
        if (packet == null) return false;
        long generation = REQUESTS.generation();
        if (packet.requestId() > 0 && REQUESTS.complete(packet.requestId(), generation, nowMillis).isEmpty()) {
            return false;
        }
        if (packet.protocolVersion() != GradleMCNetwork.PROTOCOL_VERSION) {
            serverCapabilities = new ServerCapabilityState(ServerCapabilityState.Availability.INCOMPATIBLE,
                    packet.protocolVersion(), false, false, packet.serverVersion(),
                    packet.statusMessage().isBlank() ? "GradleMC server protocol is incompatible." : packet.statusMessage(), generation);
            return false;
        }
        if (!packet.serverDiagnosticsAllowed()) {
            serverCapabilities = new ServerCapabilityState(ServerCapabilityState.Availability.INCOMPATIBLE,
                    packet.protocolVersion(), false, false, packet.serverVersion(),
                    packet.statusMessage().isBlank() ? "The connected GradleMC server rejected this protocol." : packet.statusMessage(), generation);
            return false;
        }
        serverCapabilities = new ServerCapabilityState(ServerCapabilityState.Availability.AVAILABLE,
                packet.protocolVersion(), true, packet.administrativeDiagnosticsAllowed(), packet.serverVersion(),
                "", generation);
        updateStatus(packet.status(), packet.guiStatus());
        return true;
    }

    public static SmartAIStatus latestSmartAIStatus() {
        return latestSmartAIStatus;
    }

    public static long smartAIStatusAgeMillis() {
        long updatedAt = latestSmartAIStatusUpdatedAtMillis;
        return updatedAt <= 0L ? -1L : Math.max(0L, System.currentTimeMillis() - updatedAt);
    }

    public static GuiStatusSnapshot latestGuiStatus() {
        return latestGuiStatus;
    }

    public static ServerCapabilityState serverCapabilities() {
        return serverCapabilities;
    }

    public static int pendingRequestCount() {
        return REQUESTS.pendingCount();
    }
}

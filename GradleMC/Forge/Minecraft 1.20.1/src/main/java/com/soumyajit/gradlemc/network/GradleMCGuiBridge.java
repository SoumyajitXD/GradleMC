package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.capability.CapabilityInput;
import com.soumyajit.gradlemc.capability.CapabilityResolver;
import com.soumyajit.gradlemc.capability.CapabilitySnapshot;

public final class GradleMCGuiBridge {
    private static volatile Runnable clientOpener;
    private static volatile SmartAIStatus latestSmartAIStatus = SmartAIStatus.disabled();
    private static volatile GuiStatusSnapshot latestGuiStatus = GuiStatusSnapshot.empty();
    private static volatile long latestSmartAIStatusUpdatedAtMillis;
    private static volatile long latestGuiStatusUpdatedAtMillis;
    private static volatile boolean activeWorld;
    private static volatile boolean integratedServerPresent;
    private static volatile boolean logicalServerReady;
    private static volatile boolean clientConnection;
    private static volatile boolean acceptedServerSnapshot;
    private static final SnapshotLifecycle LIFECYCLE = new SnapshotLifecycle();

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
        latestGuiStatusUpdatedAtMillis = latestSmartAIStatusUpdatedAtMillis;
    }

    /** Starts a new logical connection epoch. Must be called from the client thread. */
    public static SnapshotLifecycle.Request connected() {
        return LIFECYCLE.connect();
    }

    /** Drops all server-owned state so delayed packets cannot revive a former world. */
    public static void disconnected() {
        LIFECYCLE.disconnect();
        latestGuiStatus = GuiStatusSnapshot.empty();
        latestSmartAIStatus = SmartAIStatus.disabled();
        latestGuiStatusUpdatedAtMillis = 0L;
        acceptedServerSnapshot = false;
        activeWorld = false;
        integratedServerPresent = false;
        logicalServerReady = false;
        clientConnection = false;
    }

    public static SnapshotLifecycle.Request beginRefresh(long nowNanos) {
        return LIFECYCLE.request(nowNanos);
    }

    public static boolean acceptResponse(long epoch, long requestId, long generation, SmartAIStatus status, GuiStatusSnapshot snapshot) {
        if (!LIFECYCLE.receive(epoch, requestId, generation).accepted()) return false;
        updateStatus(status, snapshot);
        acceptedServerSnapshot = true;
        return true;
    }

    /** Called from the client tick adapter; only primitive lifecycle evidence crosses the boundary. */
    public static void updateLocalRuntime(boolean worldActive, boolean integratedPresent, boolean serverReady, boolean connected) {
        activeWorld = worldActive;
        integratedServerPresent = integratedPresent;
        logicalServerReady = serverReady;
        clientConnection = connected;
    }

    public static boolean timeout(long requestId, long nowNanos, long timeoutNanos) {
        return LIFECYCLE.timeout(requestId, nowNanos, timeoutNanos);
    }

    public static SnapshotLifecycle.RefreshState refreshState() { return LIFECYCLE.state(); }
    public static long connectionEpoch() { return LIFECYCLE.epoch(); }

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

    public static long guiStatusUpdatedAtMillis() {
        return latestGuiStatusUpdatedAtMillis;
    }

    public static CapabilitySnapshot capabilities() {
        long age = guiStatusUpdatedAtMillis() <= 0L ? Long.MAX_VALUE : Math.max(0L, System.currentTimeMillis() - guiStatusUpdatedAtMillis());
        CapabilityInput.ConnectionState state = !clientConnection ? CapabilityInput.ConnectionState.DISCONNECTED
                : acceptedServerSnapshot ? CapabilityInput.ConnectionState.CHANNEL_AVAILABLE : CapabilityInput.ConnectionState.CHANNEL_AVAILABLE;
        GuiStatusSnapshot status = latestGuiStatus;
        return CapabilityResolver.resolve(new CapabilityInput(true, activeWorld, integratedServerPresent, logicalServerReady,
                state, acceptedServerSnapshot, acceptedServerSnapshot && age > 10_000L,
                LIFECYCLE.state() == SnapshotLifecycle.RefreshState.ERROR, status.administrativeAllowed(), status.supportedOperations()));
    }
}

package com.soumyajit.gradlemc.metrics;

/** Deterministic demand and producer-parity checks; no Minecraft runtime is required. */
public final class MeasurementHubSelfTest {
    private MeasurementHubSelfTest() { }
    public static void run() {
        MeasurementHub hub = MeasurementHub.instance();
        hub.releaseAll();
        require(!hub.hasDemand(MeasurementChannel.FRAME_TIMING), "frame channel unexpectedly active");
        MeasurementSubscription first = hub.acquire(MeasurementChannel.FRAME_TIMING, "overlay", MeasurementDemand.NORMAL);
        MeasurementSubscription second = hub.acquire(MeasurementChannel.FRAME_TIMING, "gui", MeasurementDemand.DETAILED_FOREGROUND);
        require(hub.hasDemand(MeasurementChannel.FRAME_TIMING), "first consumer did not enable frame channel");
        require(hub.resolvedDemand(MeasurementChannel.FRAME_TIMING) == MeasurementDemand.DETAILED_FOREGROUND, "highest demand was not retained");
        first.close(); first.close();
        require(hub.hasDemand(MeasurementChannel.FRAME_TIMING), "one consumer released another consumer demand");
        second.close();
        require(!hub.hasDemand(MeasurementChannel.FRAME_TIMING), "final release did not stop optional frame work");
        long[] callbacks = {0};
        MeasurementSubscription frames = hub.subscribeFrames("fps-test", MeasurementDemand.DETAILED_FOREGROUND, ignored -> callbacks[0]++);
        hub.onRenderedFrame(1_000_000L); hub.onRenderedFrame(17_000_000L);
        require(callbacks[0] == 2, "one producer did not fan out exactly once per frame");
        require(hub.frameSnapshot(false).sampleCount() == 1, "frame snapshot was not populated by authoritative producer");
        frames.close(); hub.releaseOwner("fps-test");
        require(!hub.hasDemand(MeasurementChannel.FRAME_TIMING), "context owner release leaked demand");
        long[] survivors = {0};
        MeasurementSubscription failing = hub.subscribeFrames("failing", MeasurementDemand.NORMAL, ignored -> { throw new IllegalStateException("expected"); });
        MeasurementSubscription survivor = hub.subscribeFrames("survivor", MeasurementDemand.NORMAL, ignored -> survivors[0]++);
        hub.onRenderedFrame(34_000_000L);
        require(survivors[0] == 1, "subscriber failure blocked another consumer");
        hub.releaseOwner("survivor");
        hub.onRenderedFrame(51_000_000L);
        require(survivors[0] == 1, "owner release retained frame callback");
        failing.close(); survivor.close();
        MeasurementSubscription memory = hub.acquire(MeasurementChannel.JVM_MEMORY, "memory-test", MeasurementDemand.NORMAL);
        MemorySnapshot memorySnapshot = hub.memorySnapshot(false);
        require(memorySnapshot.availability() == MemorySnapshot.Availability.AVAILABLE, "memory channel did not collect");
        require(memorySnapshot.committedBytes() >= memorySnapshot.usedBytes(), "memory snapshot is inconsistent");
        memory.close();
        hub.releaseAll();
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}

package com.soumyajit.gradlemc.network;

import java.util.UUID;

public final class ClientRequestTrackerSelfTest {
    private ClientRequestTrackerSelfTest() {
    }

    public static void run() {
        ClientRequestTracker tracker = new ClientRequestTracker();
        long firstGeneration = tracker.beginConnection();
        ClientRequestTracker.Request first = tracker.begin("status", 10L).orElseThrow();
        ClientRequestTracker.Request second = tracker.begin("snapshot", 11L).orElseThrow();
        require(first.id() != second.id(), "request IDs must be unique within a connection");
        require(tracker.complete(first.id(), firstGeneration, 12L).isPresent(), "matching response must complete once");
        require(tracker.complete(first.id(), firstGeneration, 13L).isEmpty(), "duplicate response must be ignored");
        require(tracker.complete(second.id(), firstGeneration + 1, 13L).isEmpty(), "stale generation must be ignored");
        require(tracker.pendingCount() == 1, "stale response must not remove pending request");
        require(tracker.expire(6_000L) == 1, "expired request must be removed");

        tracker.begin("status", 7_000L).orElseThrow();
        long secondGeneration = tracker.beginConnection();
        require(secondGeneration != firstGeneration, "reconnection must change generation");
        require(tracker.pendingCount() == 0, "disconnect/reconnect must clear pending requests");

        for (int index = 0; index < ClientRequestTracker.MAX_PENDING; index++) {
            require(tracker.begin("bounded", 8_000L).isPresent(), "request capacity should accept bounded requests");
        }
        require(tracker.begin("overflow", 8_000L).isEmpty(), "request tracker must remain bounded");

        UUID player = UUID.randomUUID();
        require(GradleMCNetwork.tryAcceptStatusRequest(player, 10_000L), "first server request must be accepted");
        require(!GradleMCNetwork.tryAcceptStatusRequest(player, 10_001L), "burst requests must be rejected before server scheduling");
        require(GradleMCNetwork.tryAcceptStatusRequest(player, 10_250L), "request after the bounded cooldown must be accepted");
        GradleMCNetwork.clearPlayer(null);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

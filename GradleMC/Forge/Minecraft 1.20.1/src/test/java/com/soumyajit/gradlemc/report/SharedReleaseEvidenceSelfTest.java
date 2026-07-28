package com.soumyajit.gradlemc.report;

/** Evidence handoff remains immutable, optional, and explicit about freshness. */
public final class SharedReleaseEvidenceSelfTest {
    private SharedReleaseEvidenceSelfTest() { }
    public static void run() {
        long now = System.nanoTime();
        SharedReleaseEvidence.publishFps(0, Double.NaN, Double.NaN, null, null, now);
        require(SharedReleaseEvidence.snapshot().fps().state() == SharedReleaseEvidence.State.UNAVAILABLE,
                "missing client evidence must not become zero");
        SharedReleaseEvidence.publishFps(10, 60D, 58D, null, null, now);
        require(SharedReleaseEvidence.snapshot().fps().state() == SharedReleaseEvidence.State.WARMING_UP,
                "short client evidence must be warming up");
        SharedReleaseEvidence.publishFps(20, 60D, 58D, 50D, null, now - 20_000_000_000L);
        require(SharedReleaseEvidence.snapshot().fps().state() == SharedReleaseEvidence.State.STALE,
                "expired client evidence must be stale");
        require(SharedReleaseEvidence.snapshot().fps().provenance() == SharedReleaseEvidence.Provenance.CLIENT_PROVIDED,
                "client provenance is retained");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}

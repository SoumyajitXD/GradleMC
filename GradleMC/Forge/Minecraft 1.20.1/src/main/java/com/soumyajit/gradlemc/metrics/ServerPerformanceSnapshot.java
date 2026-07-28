package com.soumyajit.gradlemc.metrics;

import java.time.Instant;

/** Immutable server-authoritative tick evidence. Numeric values are NaN when unavailable. */
public record ServerPerformanceSnapshot(double currentTps, double averageTps, double currentMspt,
                                        double averageMspt, double maximumMspt, int sampleCount,
                                        Instant collectedAt, long collectedNanos, Availability availability,
                                        Freshness freshness, String physicalSide, String logicalSide,
                                        boolean dedicatedServer, String contextIdentity, String provenance,
                                        Pressure pressure, String error) {
    public enum Availability { AVAILABLE, UNAVAILABLE, FAILURE }
    public enum Freshness { FRESH, WARMING_UP, STALE }
    public enum Pressure { UNKNOWN, NORMAL, ELEVATED, HIGH, CRITICAL }

    public static ServerPerformanceSnapshot unavailable() {
        return new ServerPerformanceSnapshot(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                0, Instant.EPOCH, 0L, Availability.UNAVAILABLE, Freshness.WARMING_UP, "UNKNOWN", "SERVER",
                false, "none", "not-collected", Pressure.UNKNOWN, "Server timing has not been collected.");
    }
}

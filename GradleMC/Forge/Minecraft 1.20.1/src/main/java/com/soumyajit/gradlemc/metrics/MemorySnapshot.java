package com.soumyajit.gradlemc.metrics;

import java.time.Instant;

/** Immutable, heap-only JVM memory evidence. This deliberately does not claim native-memory use. */
public record MemorySnapshot(long usedBytes, long committedBytes, long maxBytes, long freeHeadroomBytes,
                             double usedPercent, Pressure pressure, Instant collectedAt, long collectedNanos,
                             Availability availability, Freshness freshness, String source, String provenance,
                             int trendSamples, double recentTrendBytesPerSecond) {
    public enum Pressure { UNKNOWN, NORMAL, ELEVATED, HIGH, CRITICAL }
    public enum Availability { AVAILABLE, UNAVAILABLE, FAILURE }
    public enum Freshness { FRESH, STALE, WARMING_UP }

    public static MemorySnapshot unavailable() {
        return new MemorySnapshot(0L, 0L, -1L, -1L, Double.NaN, Pressure.UNKNOWN, Instant.EPOCH, 0L,
                Availability.UNAVAILABLE, Freshness.WARMING_UP, "jvm-heap", "not-collected", 0, Double.NaN);
    }

    public long usedMiB() { return usedBytes / (1024L * 1024L); }
    public long committedMiB() { return committedBytes / (1024L * 1024L); }
    public long maxMiB() { return maxBytes < 0L ? -1L : maxBytes / (1024L * 1024L); }
    public long freeMiB() { return freeHeadroomBytes < 0L ? -1L : freeHeadroomBytes / (1024L * 1024L); }
}

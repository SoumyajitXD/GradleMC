package com.soumyajit.gradlemc.smart;

import java.time.Instant;
import java.util.Optional;

public final class SmartMetricSnapshots {
    private static CountSnapshot latestEntityScan;
    private static CountSnapshot latestBlockEntityScan;

    private SmartMetricSnapshots() {
    }

    public static void recordEntityScan(int radius, int count) {
        latestEntityScan = new CountSnapshot(radius, count, Instant.now());
        AdaptiveBaselineStore.recordEntityScan(count);
    }

    public static void recordBlockEntityScan(int radius, int count) {
        latestBlockEntityScan = new CountSnapshot(radius, count, Instant.now());
        AdaptiveBaselineStore.recordBlockEntityScan(count);
    }

    public static Optional<CountSnapshot> latestEntityScan() {
        return Optional.ofNullable(latestEntityScan);
    }

    public static Optional<CountSnapshot> latestBlockEntityScan() {
        return Optional.ofNullable(latestBlockEntityScan);
    }

    public record CountSnapshot(int radius, int count, Instant recordedAt) {
    }
}

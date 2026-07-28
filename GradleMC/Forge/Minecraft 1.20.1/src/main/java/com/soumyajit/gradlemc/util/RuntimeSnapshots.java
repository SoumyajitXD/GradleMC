package com.soumyajit.gradlemc.util;

import com.soumyajit.gradlemc.metrics.MeasurementHub;

public final class RuntimeSnapshots {
    private static final long MIB = 1024L * 1024L;

    private RuntimeSnapshots() {
    }

    public static MemorySnapshot memory() {
        com.soumyajit.gradlemc.metrics.MemorySnapshot snapshot = MeasurementHub.instance().memorySnapshot(false);
        // A foreground view must never render the hub's "not collected" sentinel as
        // a heap metric.  This remains a local client-JVM observation only.
        if (snapshot.availability() != com.soumyajit.gradlemc.metrics.MemorySnapshot.Availability.AVAILABLE) {
            snapshot = MeasurementHub.instance().memorySnapshot(true);
        }
        if (snapshot.availability() != com.soumyajit.gradlemc.metrics.MemorySnapshot.Availability.AVAILABLE) {
            return new MemorySnapshot(-1L, -1L, -1L, -1L);
        }
        long committed = snapshot.committedMiB();
        long used = snapshot.usedMiB();
        long max = snapshot.maxMiB();
        return new MemorySnapshot(Math.max(0L, used), max < 0L ? -1L : Math.max(0L, max - Math.max(0L, used)),
                Math.max(0L, committed), max);
    }

    public record MemorySnapshot(long usedMiB, long freeMiB, long totalMiB, long maxMiB) {
        public String pressureLabel() {
            double pressure = maxMiB <= 0 ? 0.0D : (double) usedMiB / maxMiB;
            if (pressure >= 0.95D) {
                return "CRITICAL";
            }
            if (pressure >= 0.80D) {
                return "WARN";
            }
            return "PASS";
        }
    }
}

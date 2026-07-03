package com.soumyajit.gradlemc.util;

public final class RuntimeSnapshots {
    private static final long MIB = 1024L * 1024L;

    private RuntimeSnapshots() {
    }

    public static MemorySnapshot memory() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory() / MIB;
        long total = runtime.totalMemory() / MIB;
        long free = runtime.freeMemory() / MIB;
        return new MemorySnapshot(total - free, free, total, max);
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

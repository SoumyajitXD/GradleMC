package com.soumyajit.gradlemc.profiler.memory;

public final class MemoryPressureTracker {
    private static final long MIB = 1024L * 1024L;

    public Snapshot snapshot() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return new Snapshot(used / MIB, runtime.maxMemory() / MIB);
    }

    public String pressureLabel(Snapshot snapshot) {
        if (snapshot == null || snapshot.maxHeapMiB() <= 0L) {
            return "unknown";
        }
        double pressure = (double) snapshot.usedHeapMiB() / snapshot.maxHeapMiB();
        if (pressure >= 0.90D) {
            return "high";
        }
        if (pressure >= 0.75D) {
            return "elevated";
        }
        return "normal";
    }

    public record Snapshot(long usedHeapMiB, long maxHeapMiB) {
    }
}

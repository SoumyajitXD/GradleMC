package com.soumyajit.gradlemc.profiler.memory;

import com.soumyajit.gradlemc.util.RuntimeSnapshots;

public final class MemoryPressureTracker {
    private static final long MIB = 1024L * 1024L;

    public Snapshot snapshot() {
        RuntimeSnapshots.MemorySnapshot memory = RuntimeSnapshots.memory();
        return new Snapshot(memory.usedMiB(), memory.totalMiB(), memory.maxMiB(), memory.nonHeapUsedMiB());
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

    public record Snapshot(long usedHeapMiB, long committedHeapMiB, long maxHeapMiB, long usedNonHeapMiB) {
    }
}

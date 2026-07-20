package com.soumyajit.gradlemc.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public final class RuntimeSnapshots {
    private static final long MIB = 1024L * 1024L;

    private RuntimeSnapshots() {
    }

    public static MemorySnapshot memory() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        long used = nonNegative(heap.getUsed());
        long committed = nonNegative(heap.getCommitted());
        long maximum = heap.getMax() < 0L ? -1L : heap.getMax();
        long free = committed < used ? 0L : committed - used;
        return new MemorySnapshot(used / MIB, free / MIB, committed / MIB, maximum < 0L ? -1L : maximum / MIB,
                nonNegative(nonHeap.getUsed()) / MIB, nonNegative(nonHeap.getCommitted()) / MIB);
    }

    private static long nonNegative(long value) { return Math.max(0L, value); }

    /** JVM heap only; totalMiB means committed heap, never physical system RAM. */
    public record MemorySnapshot(long usedMiB, long freeMiB, long totalMiB, long maxMiB, long nonHeapUsedMiB, long nonHeapCommittedMiB) {
        public String pressureLabel() {
            if (maxMiB <= 0) return "UNAVAILABLE";
            double pressure = (double) usedMiB / maxMiB;
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

package com.soumyajit.gradlemc.performance;

import com.soumyajit.gradlemc.foundation.GradleMcRuntimeExecutor;
import java.util.Arrays;

/** Bounded monotonic timings for GradleMC work, not total Minecraft CPU usage. */
public final class GradleMcOverheadMonitor {
    public enum Category { FRAME_INGESTION, OVERLAY_PREPARATION, MEMORY_COLLECTION, SERVER_TICK_INGESTION,
        MEASUREMENT_PUBLICATION, ADAPTIVE_EVALUATION, TASK_PLANNING, ANALYSIS_LANE_EXECUTION,
        FILE_LANE_EXECUTION, COORDINATION_LANE_EXECUTION, QUEUE_DELAY,
        REPORT_FORMATTING, FILE_WRITING, PACKET_ENCODING_SENDING }
    private static final int WINDOW = 64;
    private final long[][] values = new long[Category.values().length][WINDOW];
    private final int[] sizes = new int[Category.values().length];
    private final int[] next = new int[Category.values().length];
    private long rejections, coalesced, deferred;

    public synchronized void record(Category category, long elapsedNanos) {
        if (category == null || elapsedNanos < 0L) return;
        int i = category.ordinal(); values[i][next[i]] = elapsedNanos; next[i] = (next[i] + 1) % WINDOW;
        sizes[i] = Math.min(WINDOW, sizes[i] + 1);
    }
    public synchronized void rejected() { rejections = Math.min(Long.MAX_VALUE, rejections + 1); }
    public synchronized void coalesced() { coalesced = Math.min(Long.MAX_VALUE, coalesced + 1); }
    public synchronized void deferred() { deferred = Math.min(Long.MAX_VALUE, deferred + 1); }
    public synchronized Snapshot snapshot() {
        CategoryStats[] stats = new CategoryStats[Category.values().length];
        for (Category category : Category.values()) stats[category.ordinal()] = stats(category);
        return new Snapshot(stats, GradleMcRuntimeExecutor.activeWorkers(), GradleMcRuntimeExecutor.queueDepth(), rejections, coalesced, deferred, com.soumyajit.gradlemc.metrics.MeasurementHub.instance().activeSubscriptions());
    }
    private CategoryStats stats(Category category) {
        int i=category.ordinal(), n=sizes[i]; if(n==0) return new CategoryStats(category,0,Double.NaN,Double.NaN,Double.NaN,Double.NaN);
        long[] copy=Arrays.copyOf(values[i],n); Arrays.sort(copy);
        return new CategoryStats(category,n,toMillis(copy[(n-1)/2]),n < 20 ? Double.NaN : toMillis(copy[(int)Math.ceil(n*.95)-1]),toMillis(copy[n-1]),toMillis(copy[n-1]));
    }
    private static double toMillis(long nanos) { return nanos / 1_000_000D; }
    public record CategoryStats(Category category,int samples,double medianMillis,double p95Millis,double maxMillis,double windowMaxMillis) { }
    public record Snapshot(CategoryStats[] categories,int activeWorkers,int queueDepth,long rejectionCount,long coalescingOrDeferralCount,long deferredWorkCount,int activeMeasurementSubscriptions) { }
}

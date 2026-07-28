package com.soumyajit.gradlemc.metrics;

import com.soumyajit.gradlemc.client.overlay.FpsRollingStatsCalculator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * The authoritative owner of live GradleMC measurements. Producers feed this class once and
 * consumers receive immutable snapshots or demand handles; no mutable sample buffer escapes.
 */
public final class MeasurementHub {
    private static final int MAX_FRAME_LISTENERS = 4;
    private static final int MEMORY_HISTORY = 32;
    private static final MeasurementHub INSTANCE = new MeasurementHub();
    private final EnumMap<MeasurementChannel, Map<String, List<MeasurementDemand>>> demands = new EnumMap<>(MeasurementChannel.class);
    private final Map<String, FrameListener> frameListeners = new HashMap<>();
    private final FpsRollingStatsCalculator frameStats = new FpsRollingStatsCalculator(60);
    private final long[] memoryHistoryNanos = new long[MEMORY_HISTORY];
    private final long[] memoryHistoryUsed = new long[MEMORY_HISTORY];
    private int memoryHistorySize;
    private int memoryHistoryNext;
    private MemorySnapshot memorySnapshot = MemorySnapshot.unavailable();
    private final ServerPerformanceChannel serverPerformance = new ServerPerformanceChannel();

    private MeasurementHub() {
        for (MeasurementChannel channel : MeasurementChannel.values()) demands.put(channel, new HashMap<>());
    }

    public static MeasurementHub instance() { return INSTANCE; }

    public synchronized MeasurementSubscription acquire(MeasurementChannel channel, String owner, MeasurementDemand demand) {
        Objects.requireNonNull(channel, "channel");
        String safeOwner = requireOwner(owner);
        MeasurementDemand safeDemand = demand == null ? MeasurementDemand.SNAPSHOT_ONLY : demand;
        demands.get(channel).computeIfAbsent(safeOwner, ignored -> new ArrayList<>()).add(safeDemand);
        return new DemandHandle(this, channel, safeOwner, safeDemand);
    }

    public synchronized MeasurementSubscription subscribeFrames(String owner, MeasurementDemand demand, LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        String safeOwner = requireOwner(owner);
        FrameListener existing = frameListeners.get(safeOwner);
        if (existing != null) {
            existing.leases++;
            return new FrameHandle(this, safeOwner);
        }
        if (frameListeners.size() >= MAX_FRAME_LISTENERS) throw new IllegalStateException("GradleMC frame subscriber limit reached");
        acquire(MeasurementChannel.FRAME_TIMING, safeOwner, demand);
        frameListeners.put(safeOwner, new FrameListener(consumer, demand));
        return new FrameHandle(this, safeOwner);
    }

    /** Called by the one post-GUI-render producer. Returns immediately when nothing needs frames. */
    public void onRenderedFrame(long nowNanos) {
        List<LongConsumer> listeners;
        synchronized (this) {
            if (!hasDemandLocked(MeasurementChannel.FRAME_TIMING)) return;
            frameStats.recordRenderedFrame(nowNanos);
            listeners = frameListeners.values().stream().map(FrameListener::consumer).toList();
        }
        for (LongConsumer listener : listeners) {
            try { listener.accept(nowNanos); }
            catch (RuntimeException ignored) { /* A consumer cannot break the shared producer. */ }
        }
    }

    public synchronized FpsRollingStatsCalculator.Snapshot frameSnapshot(boolean includePercentiles) {
        return frameStats.snapshot(includePercentiles);
    }

    public synchronized void setFrameWindowSeconds(int seconds) { frameStats.setWindowSeconds(seconds); }
    public synchronized void pauseFrames() { frameStats.resetInterval(); }
    public synchronized void resetFrames() { frameStats.clear(); }
    public synchronized boolean hasDemand(MeasurementChannel channel) { return hasDemandLocked(channel); }
    public synchronized MeasurementDemand resolvedDemand(MeasurementChannel channel) {
        MeasurementDemand resolved = MeasurementDemand.SNAPSHOT_ONLY;
        for (List<MeasurementDemand> values : demands.get(channel).values()) for (MeasurementDemand value : values) resolved = MeasurementDemand.highest(resolved, value);
        return resolved;
    }
    public synchronized int activeSubscriptions() { return demands.values().stream().mapToInt(map -> map.values().stream().mapToInt(List::size).sum()).sum(); }
    public synchronized int activeOwners(MeasurementChannel channel) { return demands.get(channel).size(); }
    /** Returns one authoritative heap snapshot. A forced refresh is for a foreground command/report only. */
    public synchronized MemorySnapshot memorySnapshot(boolean forceRefresh) {
        long now = System.nanoTime();
        if (forceRefresh || (hasDemandLocked(MeasurementChannel.JVM_MEMORY) && memoryDue(now))) collectMemory(now);
        return withFreshness(memorySnapshot, now);
    }
    /** Returns the last immutable heap evidence without causing collection. */
    public synchronized MemorySnapshot latestMemorySnapshot() {
        return withFreshness(memorySnapshot, System.nanoTime());
    }
    public void beginServerTick(long nowNanos) { serverPerformance.beginTick(nowNanos); }
    public void completeServerTick(long nowNanos) { serverPerformance.completeTick(nowNanos); }
    public synchronized void resetServerPerformance(boolean dedicatedServer) { serverPerformance.reset(dedicatedServer); }
    public ServerPerformanceSnapshot serverPerformanceSnapshot() { return serverPerformance.snapshot(System.nanoTime()); }

    public synchronized void releaseOwner(String owner) {
        if (owner == null) return;
        for (Map<String, List<MeasurementDemand>> values : demands.values()) values.remove(owner);
        frameListeners.remove(owner);
    }
    public synchronized void releaseAll() {
        for (Map<String, List<MeasurementDemand>> values : demands.values()) values.clear();
        frameListeners.clear(); frameStats.clear(); memoryHistorySize = 0; memoryHistoryNext = 0; memorySnapshot = MemorySnapshot.unavailable();
        serverPerformance.reset(false);
    }

    private synchronized void release(MeasurementChannel channel, String owner, MeasurementDemand demand) {
        List<MeasurementDemand> values = demands.get(channel).get(owner);
        if (values == null) return;
        values.remove(demand);
        if (values.isEmpty()) demands.get(channel).remove(owner);
    }
    private boolean hasDemandLocked(MeasurementChannel channel) { return !demands.get(channel).isEmpty(); }
    private boolean memoryDue(long now) {
        if (memorySnapshot.availability() != MemorySnapshot.Availability.AVAILABLE) return true;
        long age = Math.max(0L, now - memorySnapshot.collectedNanos());
        return age >= switch (resolvedDemand(MeasurementChannel.JVM_MEMORY)) {
            case SNAPSHOT_ONLY -> Long.MAX_VALUE;
            case LOW_FREQUENCY -> 5_000_000_000L;
            case NORMAL -> 1_000_000_000L;
            case DETAILED_FOREGROUND -> 250_000_000L;
        };
    }
    private void collectMemory(long now) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long committed = Math.max(0L, runtime.totalMemory());
            long used = Math.max(0L, committed - Math.max(0L, runtime.freeMemory()));
            long max = runtime.maxMemory();
            long headroom = max <= 0L ? -1L : Math.max(0L, max - used);
            double percent = max <= 0L ? Double.NaN : Math.min(100D, used * 100D / max);
            MemorySnapshot.Pressure pressure = !Double.isFinite(percent) ? MemorySnapshot.Pressure.UNKNOWN
                    : percent >= 95D ? MemorySnapshot.Pressure.CRITICAL : percent >= 85D ? MemorySnapshot.Pressure.HIGH
                    : percent >= 70D ? MemorySnapshot.Pressure.ELEVATED : MemorySnapshot.Pressure.NORMAL;
            memoryHistoryNanos[memoryHistoryNext] = now; memoryHistoryUsed[memoryHistoryNext] = used;
            memoryHistoryNext = (memoryHistoryNext + 1) % MEMORY_HISTORY; memoryHistorySize = Math.min(MEMORY_HISTORY, memoryHistorySize + 1);
            memorySnapshot = new MemorySnapshot(used, committed, max, headroom, percent, pressure, java.time.Instant.now(), now,
                    MemorySnapshot.Availability.AVAILABLE, memoryHistorySize < 2 ? MemorySnapshot.Freshness.WARMING_UP : MemorySnapshot.Freshness.FRESH,
                    "Runtime.getRuntime", "jvm-heap-direct", memoryHistorySize, memoryTrend());
        } catch (RuntimeException exception) {
            memorySnapshot = new MemorySnapshot(0L, 0L, -1L, -1L, Double.NaN, MemorySnapshot.Pressure.UNKNOWN,
                    java.time.Instant.now(), now, MemorySnapshot.Availability.FAILURE, MemorySnapshot.Freshness.STALE,
                    "Runtime.getRuntime", "collection-failure", memoryHistorySize, Double.NaN);
        }
    }
    private double memoryTrend() {
        if (memoryHistorySize < 2) return Double.NaN;
        int oldest = (memoryHistoryNext - memoryHistorySize + MEMORY_HISTORY) % MEMORY_HISTORY;
        int newest = (memoryHistoryNext - 1 + MEMORY_HISTORY) % MEMORY_HISTORY;
        long elapsed = memoryHistoryNanos[newest] - memoryHistoryNanos[oldest];
        return elapsed <= 0L ? Double.NaN : (memoryHistoryUsed[newest] - memoryHistoryUsed[oldest]) * 1_000_000_000D / elapsed;
    }
    private static MemorySnapshot withFreshness(MemorySnapshot value, long now) {
        if (value.availability() != MemorySnapshot.Availability.AVAILABLE || value.collectedNanos() <= 0L) return value;
        MemorySnapshot.Freshness freshness = now - value.collectedNanos() > 15_000_000_000L ? MemorySnapshot.Freshness.STALE : value.freshness();
        return freshness == value.freshness() ? value : new MemorySnapshot(value.usedBytes(), value.committedBytes(), value.maxBytes(), value.freeHeadroomBytes(), value.usedPercent(), value.pressure(), value.collectedAt(), value.collectedNanos(), value.availability(), freshness, value.source(), value.provenance(), value.trendSamples(), value.recentTrendBytesPerSecond());
    }
    private static String requireOwner(String owner) { if (owner == null || owner.isBlank() || owner.length() > 64) throw new IllegalArgumentException("Invalid measurement owner"); return owner; }

    private static final class DemandHandle implements MeasurementSubscription {
        private final MeasurementHub hub; private final MeasurementChannel channel; private final String owner; private final MeasurementDemand demand;
        private boolean closed;
        private DemandHandle(MeasurementHub hub, MeasurementChannel channel, String owner, MeasurementDemand demand) { this.hub = hub; this.channel = channel; this.owner = owner; this.demand = demand; }
        @Override public synchronized void close() { if (closed) return; closed = true; hub.release(channel, owner, demand); }
    }
    private static final class FrameListener {
        private final LongConsumer consumer; private final MeasurementDemand demand; private int leases = 1;
        private FrameListener(LongConsumer consumer, MeasurementDemand demand) { this.consumer = consumer; this.demand = demand; }
        private LongConsumer consumer() { return consumer; }
    }
    private static final class FrameHandle implements MeasurementSubscription {
        private final MeasurementHub hub; private final String owner; private boolean closed;
        private FrameHandle(MeasurementHub hub, String owner) { this.hub = hub; this.owner = owner; }
        @Override public synchronized void close() { if (closed) return; closed = true; synchronized (hub) { FrameListener listener = hub.frameListeners.get(owner); if (listener == null) return; if (--listener.leases <= 0) { hub.frameListeners.remove(owner); hub.release(MeasurementChannel.FRAME_TIMING, owner, listener.demand); } } }
    }
}

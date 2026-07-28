package com.soumyajit.gradlemc.metrics;

import java.time.Instant;

/**
 * One bounded START/END server-tick timing channel. MSPT is END-START for the same tick;
 * TPS is min(20, 1000 / mean MSPT) over the retained completed-tick window.
 */
public final class ServerPerformanceChannel {
    private static final int CAPACITY = 240;
    private static final long STALE_NANOS = 15_000_000_000L;
    private final long[] samples = new long[CAPACITY];
    private long startedNanos = Long.MIN_VALUE;
    private long lastCompletedNanos;
    private int size;
    private int next;
    private long session;
    private String identity = "none";
    private boolean dedicated;
    private ServerPerformanceSnapshot snapshot = ServerPerformanceSnapshot.unavailable();

    public synchronized void reset(boolean dedicatedServer) {
        size = 0; next = 0; startedNanos = Long.MIN_VALUE; lastCompletedNanos = 0L;
        dedicated = dedicatedServer; identity = (dedicatedServer ? "dedicated" : "integrated") + "-" + (++session);
        snapshot = ServerPerformanceSnapshot.unavailable();
    }

    public synchronized void beginTick(long nowNanos) {
        if (nowNanos <= 0L) return;
        startedNanos = nowNanos;
    }

    public synchronized void completeTick(long nowNanos) {
        if (startedNanos == Long.MIN_VALUE) return;
        long elapsed = nowNanos - startedNanos;
        startedNanos = Long.MIN_VALUE;
        if (elapsed < 0L || nowNanos < lastCompletedNanos) {
            snapshot = failure(nowNanos, "Monotonic server tick clock moved backwards.");
            return;
        }
        lastCompletedNanos = nowNanos;
        samples[next] = elapsed;
        next = (next + 1) % CAPACITY;
        size = Math.min(CAPACITY, size + 1);
        snapshot = build(nowNanos);
    }

    public synchronized ServerPerformanceSnapshot snapshot(long nowNanos) {
        if (snapshot.availability() != ServerPerformanceSnapshot.Availability.AVAILABLE || lastCompletedNanos == 0L) return snapshot;
        ServerPerformanceSnapshot.Freshness freshness = nowNanos - lastCompletedNanos > STALE_NANOS
                ? ServerPerformanceSnapshot.Freshness.STALE : snapshot.freshness();
        return freshness == snapshot.freshness() ? snapshot : copyWithFreshness(snapshot, freshness);
    }

    private ServerPerformanceSnapshot build(long nowNanos) {
        long total = 0L, maximum = 0L;
        int newest = (next - 1 + CAPACITY) % CAPACITY;
        for (int i = 0; i < size; i++) { long value = samples[i]; total += value; if (value > maximum) maximum = value; }
        double mean = total / 1_000_000.0D / size;
        double current = samples[newest] / 1_000_000.0D;
        double averageTps = mean <= 0D ? 20D : Math.min(20D, 1000D / mean);
        double currentTps = current <= 0D ? 20D : Math.min(20D, 1000D / current);
        ServerPerformanceSnapshot.Pressure pressure = mean >= 100D ? ServerPerformanceSnapshot.Pressure.CRITICAL
                : mean >= 75D ? ServerPerformanceSnapshot.Pressure.HIGH : mean >= 50D ? ServerPerformanceSnapshot.Pressure.ELEVATED
                : ServerPerformanceSnapshot.Pressure.NORMAL;
        return new ServerPerformanceSnapshot(currentTps, averageTps, current, mean, maximum, size, Instant.now(), nowNanos,
                ServerPerformanceSnapshot.Availability.AVAILABLE,
                size < 20 ? ServerPerformanceSnapshot.Freshness.WARMING_UP : ServerPerformanceSnapshot.Freshness.FRESH,
                "JVM", "SERVER", dedicated, identity, "completed-server-tick-start-end", pressure, "");
    }

    private ServerPerformanceSnapshot failure(long nowNanos, String error) {
        return new ServerPerformanceSnapshot(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, size, Instant.now(), nowNanos,
                ServerPerformanceSnapshot.Availability.FAILURE, ServerPerformanceSnapshot.Freshness.STALE,
                "JVM", "SERVER", dedicated, identity, "completed-server-tick-start-end", ServerPerformanceSnapshot.Pressure.UNKNOWN, error);
    }
    private static ServerPerformanceSnapshot copyWithFreshness(ServerPerformanceSnapshot value, ServerPerformanceSnapshot.Freshness freshness) {
        return new ServerPerformanceSnapshot(value.currentTps(), value.averageTps(), value.currentMspt(), value.averageMspt(), value.maximumMspt(), value.sampleCount(), value.collectedAt(), value.collectedNanos(), value.availability(), freshness, value.physicalSide(), value.logicalSide(), value.dedicatedServer(), value.contextIdentity(), value.provenance(), value.pressure(), value.error());
    }
}

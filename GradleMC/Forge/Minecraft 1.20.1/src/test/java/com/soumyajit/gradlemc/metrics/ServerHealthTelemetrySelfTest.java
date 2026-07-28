package com.soumyajit.gradlemc.metrics;

import java.time.Instant;

/** Deterministic tick-window checks; no server or elapsed-time dependency. */
public final class ServerHealthTelemetrySelfTest {
    private ServerHealthTelemetrySelfTest() { }
    public static void run() {
        ServerHealthTelemetry telemetry = new ServerHealthTelemetry();
        check(telemetry.snapshot(0L, Instant.EPOCH).windows().get(0).noData(), "empty window is explicit");
        for (int i = 1; i <= 30; i++) telemetry.recordTick(i * 50_000_000L, i == 30 ? 200_000_000L : 50_000_000L);
        ServerHealthTelemetry.Window window = telemetry.snapshot(1_500_000_000L, Instant.EPOCH).windows().get(0);
        check(window.samples() == 30, "bounded samples retained");
        check(window.p50Mspt() == 50D && window.p95Mspt() == 50D && window.p99Mspt() == 200D, "nearest-rank percentiles");
        check(window.over100Ms() == 1 && window.over200Ms() == 0, "threshold counters are strict");
        telemetry.recordTick(1_400_000_000L, 50_000_000L);
        check(telemetry.snapshot(1_500_000_000L, Instant.EPOCH).clockAnomalies() == 1, "monotonic anomaly is counted");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

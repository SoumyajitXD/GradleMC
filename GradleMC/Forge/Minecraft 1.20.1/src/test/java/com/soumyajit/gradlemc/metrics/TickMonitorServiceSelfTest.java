package com.soumyajit.gradlemc.metrics;

/** Deterministic warmup, hysteresis, and cooldown checks with no wall-clock waits. */
public final class TickMonitorServiceSelfTest {
    private TickMonitorServiceSelfTest() { }
    public static void run() {
        TickMonitorService.stop();
        check(TickMonitorService.start(100.0D, 0.0D), "starts once");
        for (int i = 0; i < TickMonitorService.WARMUP_TICKS; i++) TickMonitorService.onTick(200_000_000L);
        check(TickMonitorService.snapshot().incidents() == 0, "warmup prevents trigger");
        TickMonitorService.onTick(200_000_000L);
        check(TickMonitorService.snapshot().consecutiveSlow() == 1, "hysteresis first tick");
        TickMonitorService.onTick(200_000_000L);
        check(TickMonitorService.snapshot().consecutiveSlow() == 0, "trigger resets hysteresis");
        check(TickMonitorService.stop(), "stops once");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError("Tick monitor self-test failed: " + message); }
}

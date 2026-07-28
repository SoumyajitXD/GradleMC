package com.soumyajit.gradlemc.metrics;

/** Synthetic-clock tests for completed-tick accounting and bounded retention. */
public final class ServerPerformanceChannelSelfTest {
    private ServerPerformanceChannelSelfTest() { }
    public static void run() {
        ServerPerformanceChannel channel = new ServerPerformanceChannel();
        channel.reset(true);
        require(channel.snapshot(1L).availability() == ServerPerformanceSnapshot.Availability.UNAVAILABLE, "unavailable must not be zero TPS");
        channel.beginTick(1_000_000L);
        channel.completeTick(51_000_000L);
        ServerPerformanceSnapshot first = channel.snapshot(51_000_000L);
        require(first.sampleCount() == 1 && first.currentMspt() == 50D, "one completed tick must produce one 50ms sample");
        require(first.freshness() == ServerPerformanceSnapshot.Freshness.WARMING_UP, "single sample must warm up");
        for (int i = 0; i < 300; i++) { long start = 100_000_000L + i * 60_000_000L; channel.beginTick(start); channel.completeTick(start + 60_000_000L); }
        ServerPerformanceSnapshot full = channel.snapshot(18_200_000_000L);
        require(full.sampleCount() == 240, "sample ring must remain bounded");
        require(full.averageMspt() == 60D && full.averageTps() < 20D, "MSPT/TPS formula is incorrect");
        require(full.dedicatedServer() && full.contextIdentity().startsWith("dedicated-"), "dedicated context identity missing");
        require(channel.snapshot(40_000_000_000L).freshness() == ServerPerformanceSnapshot.Freshness.STALE, "staleness was not applied");
        channel.reset(false);
        require(channel.snapshot(1L).availability() == ServerPerformanceSnapshot.Availability.UNAVAILABLE, "session reset leaked samples");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}

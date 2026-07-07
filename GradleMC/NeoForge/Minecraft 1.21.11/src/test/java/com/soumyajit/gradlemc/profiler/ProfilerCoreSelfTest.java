package com.soumyajit.gradlemc.profiler;

import com.soumyajit.gradlemc.profiler.report.ProfilingReportWriter;
import com.soumyajit.gradlemc.profiler.report.ProfilingSummary;
import com.soumyajit.gradlemc.profiler.sampling.StackTraceAggregator;
import com.soumyajit.gradlemc.profiler.tick.SlowTickDetector;
import com.soumyajit.gradlemc.profiler.tick.TickRecord;
import com.soumyajit.gradlemc.profiler.tick.TickSummary;
import com.soumyajit.gradlemc.profiler.tick.TickTimelineRecorder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class ProfilerCoreSelfTest {
    private ProfilerCoreSelfTest() {
    }

    public static void run() throws Exception {
        tickTimelineIsBoundedAndSummarized();
        slowTickThresholdsAreExact();
        stackTracesAggregateSafely();
        configDefaultsAreConservative();
        reportWriterCreatesTextAndJson();
    }

    private static void tickTimelineIsBoundedAndSummarized() {
        TickTimelineRecorder recorder = new TickTimelineRecorder(3, new SlowTickDetector(50.0D));
        recorder.record(tick(10.0D));
        recorder.record(tick(20.0D));
        recorder.record(tick(60.0D));
        recorder.record(tick(100.0D));
        TickSummary summary = recorder.summary();
        assertEquals(3, summary.sampleCount(), "recorder should keep bounded entries");
        assertEquals(60.0D, summary.medianMspt(), "median should use retained entries");
        assertEquals(100.0D, summary.maxMspt(), "max should be retained maximum");
        assertEquals(2, summary.slowTickCount(), "slow tick count should respect threshold");
        assertEquals(4L, recorder.totalRecorded(), "total recorded should include overwritten entries");
    }

    private static void slowTickThresholdsAreExact() {
        SlowTickDetector detector = new SlowTickDetector(75.0D);
        assertTrue(!detector.isSlow(74.99D), "below threshold should not be slow");
        assertTrue(detector.isSlow(75.0D), "threshold value should be slow");
        assertEquals(">100ms", detector.thresholdBand(120.0D), "threshold band should classify spikes");
    }

    private static void stackTracesAggregateSafely() {
        StackTraceAggregator aggregator = new StackTraceAggregator();
        aggregator.add(new StackTraceAggregator.ThreadSnapshot("Server thread", Thread.State.RUNNABLE, List.of(
                new StackTraceElement("com.example.ModThing", "tick", "ModThing.java", 12),
                new StackTraceElement("net.minecraft.server.MinecraftServer", "tickServer", "MinecraftServer.java", 1)
        )));
        aggregator.add(new StackTraceAggregator.ThreadSnapshot("Server thread", Thread.State.RUNNABLE, List.of(
                new StackTraceElement("com.example.ModThing", "tick", "ModThing.java", 12)
        )));
        aggregator.add(new StackTraceAggregator.ThreadSnapshot("empty", Thread.State.RUNNABLE, List.of()));
        assertEquals(2L, aggregator.sampleCount(), "empty stack traces should not count");
        assertTrue(!aggregator.topFrames(5).isEmpty(), "top frames should be present");
        assertTrue(aggregator.topPackages(5).stream().noneMatch(count -> Double.isNaN(count.count())),
                "counts should not produce NaN");
    }

    private static void configDefaultsAreConservative() {
        ProfilerSessionConfig config = ProfilerSessionConfig.defaults();
        assertEquals(60, config.timeoutSeconds(), "default timeout should be bounded");
        assertEquals(20, config.intervalMillis(), "default sampling interval should be conservative");
        assertEquals("server", config.threadPattern(), "default thread filter should avoid all-thread sampling");
        assertTrue(config.onlySlowTicks(), "default profile should focus slow tick windows");
    }

    private static void reportWriterCreatesTextAndJson() throws Exception {
        Path directory = Path.of("build", "self-test-game", "gradlemc", "profiles").toAbsolutePath().normalize();
        ProfilingSummary summary = new ProfilingSummary(
                "1.0.0",
                "1.21.11",
                "61.1.8",
                "21",
                "test-vendor",
                "test-os",
                2,
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1),
                Duration.ofSeconds(1),
                new TickSummary(1, 50.0D, 50.0D, 50.0D, 50.0D, 50.0D, 50.0D, 1),
                List.of(tick(50.0D)),
                List.of(),
                1L,
                List.of(new StackTraceAggregator.FrameCount("Server thread", 1L)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                100L,
                120L,
                1024L,
                0L,
                0L,
                "self-test"
        );
        ProfilingReportWriter.Result result = new ProfilingReportWriter().write(summary, ProfilerSessionConfig.defaults(), directory);
        assertTrue(result.textPath().startsWith(directory), "text profile should be written under profiles directory");
        assertTrue(result.jsonPath().startsWith(directory), "json profile should be written under profiles directory");
        assertTrue(Files.readString(result.jsonPath()).contains("\"format\": \"gradlemc-profile-v1\""), "json output should include format");
        assertTrue(Files.readString(result.textPath()).contains("Memory-lite reports pressure"), "report should avoid fake allocation claims");
    }

    private static TickRecord tick(double millis) {
        return new TickRecord(Instant.now(), millis, 20.0D, 1, 1, 1, 1, -1, 100L, 101L, 0L, 0L, false);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

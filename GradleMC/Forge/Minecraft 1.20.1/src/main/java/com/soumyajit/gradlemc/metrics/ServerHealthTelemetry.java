package com.soumyajit.gradlemc.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The sole server tick-time source for GradleMC health views.  It retains at most five minutes of
 * primitive samples; callers receive immutable summaries rather than the live ring buffer.
 */
public final class ServerHealthTelemetry {
    public static final List<Duration> WINDOWS = List.of(Duration.ofSeconds(10), Duration.ofMinutes(1), Duration.ofMinutes(5));
    private static final int MAX_SAMPLES = 6_240;
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private long lastNanos = Long.MIN_VALUE;
    private long clockAnomalies;

    public synchronized void recordTick(long finishedNanos, long durationNanos) {
        if (durationNanos < 0 || (lastNanos != Long.MIN_VALUE && finishedNanos < lastNanos)) {
            clockAnomalies++;
            return;
        }
        lastNanos = finishedNanos;
        samples.addLast(new Sample(finishedNanos, durationNanos));
        while (samples.size() > MAX_SAMPLES) samples.removeFirst();
    }

    public synchronized Snapshot snapshot(long nowNanos, Instant capturedAt) {
        return snapshot(nowNanos, capturedAt, null);
    }

    /** Captures supported Java/system metrics without shells, GC requests, or live Minecraft objects. */
    public synchronized Snapshot snapshot(long nowNanos, Instant capturedAt, Path gameDirectory) {
        List<Window> windows = new ArrayList<>(WINDOWS.size());
        for (Duration window : WINDOWS) windows.add(window(window, nowNanos));
        return new Snapshot(capturedAt, windows, clockAnomalies, jvm(gameDirectory));
    }

    private Window window(Duration duration, long nowNanos) {
        long cutoff = nowNanos - duration.toNanos();
        List<Long> values = samples.stream().filter(sample -> sample.finishedNanos >= cutoff).map(sample -> sample.durationNanos).toList();
        if (values.isEmpty()) return Window.noData(duration);
        List<Long> ordered = values.stream().sorted().toList();
        long sum = 0; int over50 = 0, over100 = 0, over200 = 0;
        for (long value : ordered) { sum += value; if (value > 50_000_000L) over50++; if (value > 100_000_000L) over100++; if (value > 200_000_000L) over200++; }
        double meanMspt = sum / 1_000_000.0D / ordered.size();
        double tps = Math.min(20.0D, meanMspt <= 0.0D ? 20.0D : 1000.0D / meanMspt);
        long expected = Math.max(1L, Math.min(MAX_SAMPLES, duration.toMillis() / 50L));
        return new Window(duration, ordered.size(), tps, meanMspt, ms(ordered.get(0)), ms(ordered.get(ordered.size() - 1)), percentile(ordered, .50D), percentile(ordered, .95D), percentile(ordered, .99D), over50, over100, over200, ms(ordered.get(ordered.size() - 1)), ordered.size() >= Math.min(expected, 20L), false);
    }

    private static double percentile(List<Long> ordered, double percentile) {
        int index = Math.min(ordered.size() - 1, Math.max(0, (int) Math.ceil(percentile * ordered.size()) - 1));
        return ms(ordered.get(index));
    }
    private static double ms(long nanos) { return nanos / 1_000_000.0D; }

    private static Jvm jvm(Path gameDirectory) {
        try {
            Runtime runtime = Runtime.getRuntime();
            MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
            long gcCount = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean -> Math.max(0L, bean.getCollectionCount())).sum();
            long gcMillis = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean -> Math.max(0L, bean.getCollectionTime())).sum();
            double processCpu = -1.0D, systemCpu = -1.0D;
            java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean supported) {
                processCpu = supported.getProcessCpuLoad();
                systemCpu = supported.getCpuLoad();
            }
            long diskTotal = -1L, diskUsable = -1L;
            String limitation = "CPU and disk are unavailable on this JVM/runtime.";
            if (gameDirectory != null && Files.isDirectory(gameDirectory)) {
                try { FileStore store = Files.getFileStore(gameDirectory); diskTotal = store.getTotalSpace(); diskUsable = store.getUsableSpace(); limitation = processCpu < 0.0D ? "CPU metrics unavailable on this JVM." : "Last GC timestamp is not exposed by the standard Java management APIs."; }
                catch (Exception ignored) { limitation = "Game-directory disk metrics unavailable; last GC timestamp is not exposed by the standard Java management APIs."; }
            } else if (processCpu >= 0.0D) limitation = "Game-directory disk metrics unavailable; last GC timestamp is not exposed by the standard Java management APIs.";
            return new Jvm(heap.getUsed(), heap.getCommitted(), heap.getMax(), nonHeap.getUsed(), nonHeap.getCommitted(), nonHeap.getMax(), gcCount, gcMillis, ManagementFactory.getThreadMXBean().getThreadCount(), ManagementFactory.getThreadMXBean().getDaemonThreadCount(), ManagementFactory.getThreadMXBean().getPeakThreadCount(), ManagementFactory.getRuntimeMXBean().getUptime(), runtime.availableProcessors(), processCpu, systemCpu, diskTotal, diskUsable, Instant.now(), limitation);
        } catch (RuntimeException unsupported) {
            return Jvm.unavailable();
        }
    }

    private record Sample(long finishedNanos, long durationNanos) { }
    public record Snapshot(Instant capturedAt, List<Window> windows, long clockAnomalies, Jvm jvm) { public Snapshot { windows = List.copyOf(windows); } }
    public record Window(Duration duration, int samples, double tps, double meanMspt, double minMspt, double maxMspt, double p50Mspt, double p95Mspt, double p99Mspt, int over50Ms, int over100Ms, int over200Ms, double longestMspt, boolean complete, boolean noData) {
        static Window noData(Duration duration) { return new Window(duration, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, true); }
    }
    public record Jvm(long heapUsed, long heapCommitted, long heapMax, long nonHeapUsed, long nonHeapCommitted, long nonHeapMax, long gcCount, long gcMillis, int liveThreads, int daemonThreads, int peakThreads, long uptimeMillis, int processors, double processCpuLoad, double systemCpuLoad, long diskTotal, long diskUsable, Instant capturedAt, String limitation) {
        static Jvm unavailable() { return new Jvm(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1.0D, -1.0D, -1L, -1L, Instant.EPOCH, "Java management APIs unavailable."); }
    }
}

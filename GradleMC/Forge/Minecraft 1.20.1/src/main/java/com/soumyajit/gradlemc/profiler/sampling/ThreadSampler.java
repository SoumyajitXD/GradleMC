package com.soumyajit.gradlemc.profiler.sampling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ThreadSampler implements AutoCloseable {
    private static final int RECENT_SNAPSHOT_LIMIT = 128;
    public static final int MAX_MATCHED_THREADS = 16;
    public static final int MAX_FRAMES_PER_THREAD = 64;
    private final StackTraceAggregator aggregator;
    private final int intervalMillis;
    private final String threadPattern;
    private final boolean includeSleeping;
    private final boolean aggregateImmediately;
    private final Deque<StackTraceAggregator.ThreadSnapshot> recentSnapshots = new ArrayDeque<>();
    /*
     * Sampling is deliberately driven by the already-owned server tick.  A CPU-lite
     * profile must never introduce its own periodic executor: ScheduledThreadPoolExecutor
     * uses an unbounded delayed-work queue and used to leave a sampler thread alive during
     * shutdown races.  This class retains only immutable stack snapshots and has no worker.
     */
    private boolean running;
    private long capturedSamples;
    private long samplingPasses,missedPasses,maxDriftNanos,droppedRecentSnapshots,droppedThreads,truncatedFrames,failures,nextExpectedNanos;
    private String lastFailure="";

    public ThreadSampler(StackTraceAggregator aggregator, int intervalMillis, String threadPattern,
                         boolean includeSleeping, boolean aggregateImmediately) {
        this.aggregator = aggregator;
        this.intervalMillis = Math.max(4, intervalMillis);
        this.threadPattern = threadPattern == null || threadPattern.isBlank() ? "server" : threadPattern;
        this.includeSleeping = includeSleeping;
        this.aggregateImmediately = aggregateImmediately;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        nextExpectedNanos=System.nanoTime();
    }

    /** Called from the profiler's existing server-tick consumer. */
    public void onServerTick(long nowNanos) {
        synchronized (this) {
            if (!running || nowNanos < nextExpectedNanos) return;
        }
        sampleSafely(nowNanos);
    }

    public synchronized void commitRecent() {
        aggregator.addAll(new ArrayList<>(recentSnapshots));
        recentSnapshots.clear();
    }

    public synchronized long capturedSamples() {
        return capturedSamples;
    }
    public synchronized SamplingStats stats(){return new SamplingStats(capturedSamples,samplingPasses,missedPasses,maxDriftNanos,droppedRecentSnapshots,droppedThreads,truncatedFrames,failures,lastFailure,recentSnapshots.size());}

    @Override
    public synchronized void close() {
        stop();
    }

    /** Non-blocking stop for the server tick.  The report worker may await the sampler later. */
    public synchronized void stop() {
        running = false;
    }

    /** Kept for compatibility with finalization callers; no thread needs joining. */
    public boolean awaitStopped(long timeout, java.util.concurrent.TimeUnit unit) { return true; }

    private void sampleSafely(long now) {
        synchronized(this){long drift=Math.max(0,now-nextExpectedNanos);maxDriftNanos=Math.max(maxDriftNanos,drift);if(drift>=intervalMillis*2_000_000L)missedPasses+=Math.max(0,drift/(intervalMillis*1_000_000L)-1);nextExpectedNanos=now+intervalMillis*1_000_000L;samplingPasses++;}
        try {
            List<StackTraceAggregator.ThreadSnapshot> snapshots = capture();
            synchronized (this) {
                for (StackTraceAggregator.ThreadSnapshot snapshot : snapshots) {
                    capturedSamples++;
                    if (aggregateImmediately) {
                        aggregator.add(snapshot);
                    } else {
                        recentSnapshots.addLast(snapshot);
                        while (recentSnapshots.size() > RECENT_SNAPSHOT_LIMIT) {
                            recentSnapshots.removeFirst();
                            droppedRecentSnapshots++;
                        }
                    }
                }
            }
        } catch (RuntimeException exception) {
            synchronized(this){failures++;lastFailure=exception.getClass().getSimpleName();}
        }
    }
    public record SamplingStats(long capturedSamples,long samplingPasses,long missedPasses,long maxDriftNanos,long droppedRecentSnapshots,long droppedThreads,long truncatedFrames,long failures,String lastFailure,int retainedRecentSnapshots){ }

    private List<StackTraceAggregator.ThreadSnapshot> capture() {
        List<StackTraceAggregator.ThreadSnapshot> snapshots = new ArrayList<>();
        List<Map.Entry<Thread, StackTraceElement[]>> entries = new ArrayList<>(Thread.getAllStackTraces().entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().getName()));
        for (Map.Entry<Thread, StackTraceElement[]> entry : entries) {
            Thread thread = entry.getKey();
            if (!matches(thread.getName()) || shouldSkipState(thread.getState())) {
                continue;
            }
            if (snapshots.size() >= MAX_MATCHED_THREADS) {
                synchronized (this) { droppedThreads++; }
                continue;
            }
            StackTraceElement[] stack = entry.getValue();
            if (stack.length == 0) {
                continue;
            }
            if (stack.length > MAX_FRAMES_PER_THREAD) {
                synchronized (this) { truncatedFrames += stack.length - MAX_FRAMES_PER_THREAD; }
                stack = Arrays.copyOf(stack, MAX_FRAMES_PER_THREAD);
            }
            snapshots.add(new StackTraceAggregator.ThreadSnapshot(thread.getName(), thread.getState(), List.of(stack)));
        }
        return snapshots;
    }

    private boolean shouldSkipState(Thread.State state) {
        return !includeSleeping && (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING);
    }

    private boolean matches(String threadName) {
        String pattern = threadPattern.trim();
        if ("*".equals(pattern) || "all".equalsIgnoreCase(pattern)) {
            return true;
        }
        String lowerName = threadName.toLowerCase(Locale.ROOT);
        String lowerPattern = pattern.toLowerCase(Locale.ROOT);
        if ("server".equals(lowerPattern) || "server thread".equals(lowerPattern)) {
            return lowerName.contains("server");
        }
        if ("render".equals(lowerPattern) || "render thread".equals(lowerPattern)) {
            return lowerName.contains("render") || lowerName.contains("client");
        }
        return lowerName.contains(lowerPattern);
    }
}

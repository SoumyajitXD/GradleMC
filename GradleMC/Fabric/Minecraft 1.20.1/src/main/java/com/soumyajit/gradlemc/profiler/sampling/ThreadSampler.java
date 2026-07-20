package com.soumyajit.gradlemc.profiler.sampling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ThreadSampler implements AutoCloseable {
    private static final int RECENT_SNAPSHOT_LIMIT = 128;
    private final StackTraceAggregator aggregator;
    private final int intervalMillis;
    private final String threadPattern;
    private final boolean includeSleeping;
    private final boolean aggregateImmediately;
    private final Deque<StackTraceAggregator.ThreadSnapshot> recentSnapshots = new ArrayDeque<>();
    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture<?> scheduledSample;
    private long capturedSamples;

    public ThreadSampler(StackTraceAggregator aggregator, int intervalMillis, String threadPattern,
                         boolean includeSleeping, boolean aggregateImmediately) {
        this.aggregator = aggregator;
        this.intervalMillis = Math.max(4, intervalMillis);
        this.threadPattern = threadPattern == null || threadPattern.isBlank() ? "server" : threadPattern;
        this.includeSleeping = includeSleeping;
        this.aggregateImmediately = aggregateImmediately;
    }

    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "GradleMC CPU-lite sampler");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((failed, error) -> com.soumyajit.gradlemc.GradleMC.LOGGER.error("GradleMC sampler failed", error));
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduledSample = executor.scheduleAtFixedRate(this::sampleSafely, 0L, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized void commitRecent() {
        aggregator.addAll(new ArrayList<>(recentSnapshots));
        recentSnapshots.clear();
    }

    public synchronized long capturedSamples() {
        return capturedSamples;
    }

    @Override
    public synchronized void close() {
        if (executor != null) {
            if (scheduledSample != null) scheduledSample.cancel(true);
            executor.shutdownNow();
            executor.purge();
            scheduledSample = null;
            executor = null;
        }
    }

    private void sampleSafely() {
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
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Sampling must never crash the server.
        }
    }

    private List<StackTraceAggregator.ThreadSnapshot> capture() {
        List<StackTraceAggregator.ThreadSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            if (!matches(thread.getName()) || shouldSkipState(thread.getState())) {
                continue;
            }
            StackTraceElement[] stack = entry.getValue();
            if (stack.length == 0) {
                continue;
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

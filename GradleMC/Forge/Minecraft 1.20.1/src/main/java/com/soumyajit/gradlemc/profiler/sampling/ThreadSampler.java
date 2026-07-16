package com.soumyajit.gradlemc.profiler.sampling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadSampler implements AutoCloseable {
    private static final int RECENT_SNAPSHOT_LIMIT = 128;
    private final StackTraceAggregator aggregator;
    private final int intervalMillis;
    private final String threadPattern;
    private final boolean includeSleeping;
    private final boolean aggregateImmediately;
    private final Deque<StackTraceAggregator.ThreadSnapshot> recentSnapshots = new ArrayDeque<>();
    private ScheduledExecutorService executor;
    private long capturedSamples;
    private long samplingPasses,missedPasses,maxDriftNanos,droppedRecentSnapshots,failures,nextExpectedNanos;
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
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "GradleMC CPU-lite sampler");
            thread.setDaemon(true);
            return thread;
        });
        nextExpectedNanos=System.nanoTime();
        executor.scheduleAtFixedRate(this::sampleSafely, 0L, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized void commitRecent() {
        aggregator.addAll(new ArrayList<>(recentSnapshots));
        recentSnapshots.clear();
    }

    public synchronized long capturedSamples() {
        return capturedSamples;
    }
    public synchronized SamplingStats stats(){return new SamplingStats(capturedSamples,samplingPasses,missedPasses,maxDriftNanos,droppedRecentSnapshots,failures,lastFailure,recentSnapshots.size());}

    @Override
    public synchronized void close() {
        if (executor != null) {
            executor.shutdownNow();
            try{executor.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException exception){Thread.currentThread().interrupt();}
            executor = null;
        }
    }

    private void sampleSafely() {
        long now=System.nanoTime();
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
    public record SamplingStats(long capturedSamples,long samplingPasses,long missedPasses,long maxDriftNanos,long droppedRecentSnapshots,long failures,String lastFailure,int retainedRecentSnapshots){ }

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

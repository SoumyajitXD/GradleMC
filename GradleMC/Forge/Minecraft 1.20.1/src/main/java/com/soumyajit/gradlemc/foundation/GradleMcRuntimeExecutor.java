package com.soumyajit.gradlemc.foundation;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The single owner of GradleMC background execution.  Live Minecraft objects never cross these
 * lanes; callers capture immutable evidence through {@link ForgeGameThreadBridgeService} first.
 */
public final class GradleMcRuntimeExecutor {
    public enum Lane { SHORT_ANALYSIS, FILE_WORK, DIAGNOSTIC_COORDINATION }

    private static volatile Lanes lanes;

    private GradleMcRuntimeExecutor() { }

    public static synchronized void start() {
        if (lanes == null || lanes.stopping()) lanes = new Lanes();
    }

    public static ThreadPoolExecutor lane(Lane lane) {
        start();
        return switch (lane) {
            case SHORT_ANALYSIS -> lanes.shortAnalysis;
            case FILE_WORK -> lanes.fileWork;
            case DIAGNOSTIC_COORDINATION -> lanes.coordination;
        };
    }

    /** Rejects visibly and never applies caller-runs, including when called from a game thread. */
    public static void execute(Lane lane, Runnable work) {
        try {
            lane(lane).execute(work);
        } catch (RejectedExecutionException exception) {
            throw new RejectedExecutionException("GradleMC bounded " + lane.name().toLowerCase()
                    + " capacity is occupied or shutting down; try again shortly.", exception);
        }
    }

    public static int queueDepth() {
        Lanes current = lanes;
        return current == null ? 0 : current.shortAnalysis.getQueue().size() + current.fileWork.getQueue().size()
                + current.coordination.getQueue().size();
    }

    public static int activeWorkers() {
        Lanes current = lanes;
        return current == null ? 0 : current.shortAnalysis.getActiveCount() + current.fileWork.getActiveCount()
                + current.coordination.getActiveCount();
    }

    public static synchronized void shutdown() {
        Lanes current = lanes;
        if (current == null) return;
        current.shutdown();
    }

    private static final class Lanes {
        final ThreadPoolExecutor shortAnalysis = worker("analysis", 1, 4);
        final ThreadPoolExecutor fileWork = worker("file", 1, 4);
        final ThreadPoolExecutor coordination = worker("coordination", 1, 2);

        boolean stopping() { return shortAnalysis.isShutdown() || fileWork.isShutdown() || coordination.isShutdown(); }
        void shutdown() {
            shortAnalysis.shutdown(); fileWork.shutdown(); coordination.shutdown();
            await(shortAnalysis); await(fileWork); await(coordination);
        }
        private static void await(ThreadPoolExecutor worker) {
            try { if (!worker.awaitTermination(2, TimeUnit.SECONDS)) worker.shutdownNow(); }
            catch (InterruptedException exception) { Thread.currentThread().interrupt(); worker.shutdownNow(); }
        }
        private static ThreadPoolExecutor worker(String name, int threads, int capacity) {
            AtomicInteger sequence = new AtomicInteger();
            ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(capacity), task -> {
                        Thread thread = new Thread(task, "GradleMC-" + name + "-" + sequence.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }, new ThreadPoolExecutor.AbortPolicy());
            executor.allowCoreThreadTimeOut(true);
            return executor;
        }
    }
}

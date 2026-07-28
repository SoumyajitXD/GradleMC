package com.soumyajit.gradlemc.profiler;

import com.soumyajit.gradlemc.profiler.memory.GcEventTracker;
import com.soumyajit.gradlemc.metrics.ServerPerformanceSnapshot;
import com.soumyajit.gradlemc.profiler.memory.MemoryPressureTracker;
import com.soumyajit.gradlemc.profiler.report.ProfilingReportWriter;
import com.soumyajit.gradlemc.profiler.report.ProfilingSummary;
import com.soumyajit.gradlemc.profiler.report.ProfilingSummaryBuilder;
import com.soumyajit.gradlemc.profiler.sampling.StackTraceAggregator;
import com.soumyajit.gradlemc.profiler.sampling.ThreadSampler;
import com.soumyajit.gradlemc.profiler.tick.SlowTickDetector;
import com.soumyajit.gradlemc.profiler.tick.SlowTickSnapshot;
import com.soumyajit.gradlemc.profiler.tick.TickRecord;
import com.soumyajit.gradlemc.profiler.tick.TickTimelineRecorder;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public final class ProfilerSession {
    private static final int TICK_RING_CAPACITY = 12_000;
    private static final int SLOW_SNAPSHOT_LIMIT = 100;

    private final ProfilerSessionConfig config;
    private final Instant startedAt;
    private final TickTimelineRecorder tickRecorder;
    private final SlowTickDetector slowTickDetector;
    private final StackTraceAggregator stackAggregator = new StackTraceAggregator();
    private final MemoryPressureTracker memoryTracker = new MemoryPressureTracker();
    private final GcEventTracker gcTracker = new GcEventTracker();
    private final List<SlowTickSnapshot> slowTickSnapshots = new ArrayList<>();
    private final MemoryPressureTracker.Snapshot memoryStart;
    private final GcEventTracker.Snapshot gcStart;
    private ThreadSampler sampler;
    private ProfilerSessionState state = ProfilerSessionState.RUNNING;
    private Instant endedAt;
    private MemoryPressureTracker.Snapshot tickMemoryBefore;
    private GcEventTracker.Snapshot tickGcBefore;
    private long heapEndMiB;
    private long gcCountDelta;
    private long gcTimeDeltaMillis;
    private ProfilingReportWriter.Result reportResult;
    private ProfilingSummary summary;

    public ProfilerSession(ProfilerSessionConfig config, Instant startedAt) {
        this.config = config.sanitized();
        this.startedAt = startedAt;
        this.slowTickDetector = new SlowTickDetector(this.config.onlyTicksOverMillis() <= 0.0D ? 50.0D : this.config.onlyTicksOverMillis());
        this.tickRecorder = new TickTimelineRecorder(TICK_RING_CAPACITY, slowTickDetector);
        this.memoryStart = memoryTracker.snapshot();
        this.gcStart = gcTracker.snapshot();
        if (this.config.mode().samplesCpu()) {
            boolean aggregateImmediately = !this.config.onlySlowTicks();
            this.sampler = new ThreadSampler(stackAggregator, this.config.intervalMillis(), this.config.threadPattern(),
                    this.config.includeSleeping(), aggregateImmediately);
            this.sampler.start();
        }
    }

    /**
     * Records the shared completed-server-tick evidence. This session may add bounded
     * contextual aggregation, but never owns an ordinary server-timing producer.
     */
    public boolean onTickEnd(MinecraftServer server, ServerPerformanceSnapshot performance) {
        if (state != ProfilerSessionState.RUNNING) {
            return false;
        }
        if (sampler != null) {
            sampler.onServerTick(System.nanoTime());
        }
        tickMemoryBefore = memoryTracker.snapshot();
        tickGcBefore = gcTracker.snapshot();
        double durationMs = performance != null
                && performance.availability() == ServerPerformanceSnapshot.Availability.AVAILABLE
                && Double.isFinite(performance.currentMspt())
                ? performance.currentMspt() : Double.NaN;
        MemoryPressureTracker.Snapshot memoryAfter = memoryTracker.snapshot();
        GcEventTracker.Snapshot gcAfter = gcTracker.snapshot();
        GcEventTracker.Delta tickGc = gcAfter.delta(tickGcBefore);
        TickRecord record = new TickRecord(
                Instant.now(),
                durationMs,
                Double.isFinite(durationMs) && durationMs > 0.0D ? Math.min(20.0D, 1000.0D / durationMs) : Double.NaN,
                dimensionCount(server),
                server.getPlayerCount(),
                loadedChunkCount(server),
                entityCount(server),
                -1,
                tickMemoryBefore == null ? memoryAfter.usedHeapMiB() : tickMemoryBefore.usedHeapMiB(),
                memoryAfter.usedHeapMiB(),
                tickGc.collectionCount(),
                tickGc.collectionTimeMillis(),
                false
        );
        if (config.mode().recordsTicks() && Double.isFinite(durationMs)) {
            tickRecorder.record(record);
        }
        if (Double.isFinite(durationMs) && slowTickDetector.isSlow(durationMs)) {
            if (sampler != null && config.onlySlowTicks()) {
                sampler.commitRecent();
            }
            recordSlowTick(record);
        }
        return Duration.between(startedAt, Instant.now()).getSeconds() >= config.timeoutSeconds();
    }

    public synchronized ProfilingReportWriter.Result finish(ProfilerSessionState endState) throws IOException {
        ProfilingSummary ready = finishSummary(endState);
        if (reportResult == null) reportResult = new ProfilingReportWriter().write(ready, config, GradleMcPaths.profileDirectory());
        return reportResult;
    }

    /** Finalizes bounded in-memory evidence.  Report serialization may safely happen on a worker. */
    /** A server-tick-safe transition: it signals the sampler but never waits for it. */
    public synchronized void prepareFinish(ProfilerSessionState endState) {
        if (state != ProfilerSessionState.RUNNING) return;
        state = endState;
        endedAt = Instant.now();
        if (sampler != null) {
            sampler.stop();
        }
    }

    /** Off-thread finalization for the bounded summary and report writer. */
    public synchronized ProfilingSummary finishSummary(ProfilerSessionState endState) {
        prepareFinish(endState);
        if (summary != null) return summary;
        if (sampler != null) sampler.awaitStopped(2, java.util.concurrent.TimeUnit.SECONDS);
        MemoryPressureTracker.Snapshot memoryEnd = memoryTracker.snapshot();
        GcEventTracker.Delta gcDelta = gcTracker.snapshot().delta(gcStart);
        heapEndMiB = memoryEnd.usedHeapMiB();
        gcCountDelta = gcDelta.collectionCount();
        gcTimeDeltaMillis = gcDelta.collectionTimeMillis();
        summary = new ProfilingSummaryBuilder().build(this);
        return summary;
    }

    public synchronized void cancel() {
        prepareFinish(ProfilerSessionState.CANCELLED);
    }

    private void recordSlowTick(TickRecord record) {
        if (slowTickSnapshots.size() >= SLOW_SNAPSHOT_LIMIT) {
            slowTickSnapshots.remove(0);
        }
        String category = likelyCategory(record);
        String confidence = "LOW";
        if (record.gcCountDelta() > 0L || record.gcTimeDeltaMillis() > 0L) {
            confidence = "MEDIUM";
        }
        slowTickSnapshots.add(new SlowTickSnapshot(
                record.timestamp(),
                record.durationMillis(),
                "server",
                record.loadedChunkCount(),
                record.entityCount(),
                record.blockEntityCount(),
                record.heapUsedAfterMiB(),
                record.gcCountDelta(),
                record.gcTimeDeltaMillis(),
                category,
                confidence,
                stackAggregator.topLeaves(5)
        ));
    }

    private static String likelyCategory(TickRecord record) {
        if (record.gcCountDelta() > 0L || record.gcTimeDeltaMillis() > 0L) {
            return "GC/memory pressure";
        }
        if (record.entityCount() > 5000) {
            return "entity load";
        }
        if (record.loadedChunkCount() > 0 && record.durationMillis() >= 150.0D) {
            return "chunk/worldgen load";
        }
        return "unknown";
    }

    private static int dimensionCount(MinecraftServer server) {
        return (int) StreamSupport.stream(server.getAllLevels().spliterator(), false).count();
    }

    private static int loadedChunkCount(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            count += level.getChunkSource().getLoadedChunksCount();
        }
        return count;
    }

    private static int entityCount(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            count += level.getEntities().getAll().spliterator().getExactSizeIfKnown() > 0
                    ? (int) level.getEntities().getAll().spliterator().getExactSizeIfKnown()
                    : 0;
        }
        return count;
    }

    public ProfilerSessionConfig config() {
        return config;
    }

    public ProfilerSessionState state() {
        return state;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt == null ? Instant.now() : endedAt;
    }

    public TickTimelineRecorder tickRecorder() {
        return tickRecorder;
    }

    public StackTraceAggregator stackAggregator() {
        return stackAggregator;
    }

    public List<SlowTickSnapshot> slowTickSnapshots() {
        return List.copyOf(slowTickSnapshots);
    }

    public long heapStartMiB() {
        return memoryStart.usedHeapMiB();
    }

    public long heapEndMiB() {
        return heapEndMiB;
    }

    public long maxHeapMiB() {
        return memoryStart.maxHeapMiB();
    }

    public long gcCountDelta() {
        return gcCountDelta;
    }

    public long gcTimeDeltaMillis() {
        return gcTimeDeltaMillis;
    }

    public ProfilingReportWriter.Result reportResult() {
        return reportResult;
    }
}

package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.smart.StabilityScore;
import java.time.Instant;

/**
 * Small process-local handoff for already-published diagnostic evidence.  It deliberately never
 * starts collection: consumers receive the most recent immutable snapshots or an explicit state.
 */
public final class SharedReleaseEvidence {
    public enum State { FRESH, STALE, WARMING_UP, UNAVAILABLE, UNSUPPORTED, COLLECTION_FAILED }
    public enum Provenance { CLIENT_PROVIDED, SERVER_PROVIDED, LOCAL_GRADLEMC }
    private static final long STALE_NANOS = 15_000_000_000L;
    private static volatile Fps fps = Fps.unavailable();
    private static volatile Stability stability = Stability.unavailable();
    private static volatile AdaptiveRisk adaptiveRisk = AdaptiveRisk.unavailable();

    private SharedReleaseEvidence() { }

    /** Primitive common-side handoff keeps dedicated-server class loading independent of client overlay classes. */
    public static void publishFps(int sampleCount, double currentFps, double averageFps, Double onePercentLow,
                                  Double pointOnePercentLow, long nowNanos) {
        State state = sampleCount == 0 ? State.UNAVAILABLE : sampleCount < 20 ? State.WARMING_UP : State.FRESH;
        fps = new Fps(currentFps, averageFps, onePercentLow, pointOnePercentLow, sampleCount,
                state, Provenance.CLIENT_PROVIDED, Instant.now(), nowNanos);
    }
    public static void publishStability(StabilityScore value) {
        if (value == null) return;
        stability = new Stability(value.score(), value.riskLevel().name(), value.confidence().name(),
                value.findings().size(), stateFor(value.missingDataNotes().isEmpty()), Provenance.LOCAL_GRADLEMC,
                Instant.now(), System.nanoTime());
    }
    public static void publishAdaptiveRisk(SmartAIStatus value) {
        if (value == null) return;
        adaptiveRisk = new AdaptiveRisk(value.threatScore(), value.threatLevel().name(), value.topRiskFactors(),
                value.adaptiveSmartAIEnabled() ? State.FRESH : State.UNSUPPORTED, Provenance.SERVER_PROVIDED,
                Instant.now(), System.nanoTime());
    }
    public static Snapshot snapshot() {
        long now = System.nanoTime();
        return new Snapshot(freshness(fps, now), freshness(stability, now), freshness(adaptiveRisk, now), Instant.now(), now);
    }
    private static State stateFor(boolean complete) { return complete ? State.FRESH : State.WARMING_UP; }
    private static Fps freshness(Fps value, long now) { return value.withState(freshness(value.state(), value.capturedNanos(), now)); }
    private static Stability freshness(Stability value, long now) { return value.withState(freshness(value.state(), value.capturedNanos(), now)); }
    private static AdaptiveRisk freshness(AdaptiveRisk value, long now) { return value.withState(freshness(value.state(), value.capturedNanos(), now)); }
    private static State freshness(State state, long captured, long now) {
        return state == State.FRESH && captured > 0L && now - captured > STALE_NANOS ? State.STALE : state;
    }

    public record Snapshot(Fps fps, Stability stability, AdaptiveRisk adaptiveRisk, Instant capturedAt, long capturedNanos) { }
    public record Fps(double current, double average, Double onePercentLow, Double pointOnePercentLow, int samples,
                      State state, Provenance provenance, Instant capturedAt, long capturedNanos) {
        static Fps unavailable() { return new Fps(Double.NaN, Double.NaN, null, null, 0, State.UNAVAILABLE,
                Provenance.CLIENT_PROVIDED, Instant.EPOCH, 0L); }
        Fps withState(State replacement) { return replacement == state ? this : new Fps(current, average, onePercentLow, pointOnePercentLow, samples, replacement, provenance, capturedAt, capturedNanos); }
    }
    public record Stability(int score, String risk, String confidence, int findings, State state,
                            Provenance provenance, Instant capturedAt, long capturedNanos) {
        static Stability unavailable() { return new Stability(-1, "unavailable", "unavailable", 0, State.UNAVAILABLE,
                Provenance.LOCAL_GRADLEMC, Instant.EPOCH, 0L); }
        Stability withState(State replacement) { return replacement == state ? this : new Stability(score, risk, confidence, findings, replacement, provenance, capturedAt, capturedNanos); }
    }
    public record AdaptiveRisk(int score, String classification, String factors, State state,
                               Provenance provenance, Instant capturedAt, long capturedNanos) {
        static AdaptiveRisk unavailable() { return new AdaptiveRisk(-1, "unavailable", "", State.UNAVAILABLE,
                Provenance.SERVER_PROVIDED, Instant.EPOCH, 0L); }
        AdaptiveRisk withState(State replacement) { return replacement == state ? this : new AdaptiveRisk(score, classification, factors, replacement, provenance, capturedAt, capturedNanos); }
    }
}

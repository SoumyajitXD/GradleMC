package com.soumyajit.gradlemc.network;

import java.util.Objects;

/**
 * Client-side ordering and refresh state for immutable server dashboard snapshots.
 * All methods are called from the client thread after packet dispatch.
 */
public final class SnapshotLifecycle {
    public enum RefreshState { IDLE, REQUESTING, RECEIVED, TIMED_OUT, DISCONNECTED, INCOMPATIBLE, ERROR }

    public record Request(long epoch, long requestId) { }
    public record Decision(boolean accepted, RefreshState state) { }

    private long epoch = 1L;
    private long nextRequestId;
    private long acceptedGeneration;
    private long pendingRequestId;
    private long pendingStartedNanos;
    private RefreshState state = RefreshState.DISCONNECTED;

    public Request connect() {
        epoch = nextPositive(epoch);
        nextRequestId = 0L;
        acceptedGeneration = 0L;
        pendingRequestId = 0L;
        pendingStartedNanos = 0L;
        state = RefreshState.IDLE;
        return new Request(epoch, 0L);
    }

    public void disconnect() {
        epoch = nextPositive(epoch);
        pendingRequestId = 0L;
        pendingStartedNanos = 0L;
        acceptedGeneration = 0L;
        state = RefreshState.DISCONNECTED;
    }

    public Request request(long nowNanos) {
        if (state == RefreshState.DISCONNECTED || nowNanos < 0L) return new Request(epoch, 0L);
        pendingRequestId = nextRequestId = nextPositive(nextRequestId);
        pendingStartedNanos = nowNanos;
        state = RefreshState.REQUESTING;
        return new Request(epoch, pendingRequestId);
    }

    public Decision receive(long responseEpoch, long requestId, long generation) {
        if (responseEpoch != epoch || requestId <= 0L || generation <= 0L) return new Decision(false, state);
        if (pendingRequestId != requestId || state != RefreshState.REQUESTING) return new Decision(false, state);
        if (acceptedGeneration != 0L && !after(generation, acceptedGeneration)) return new Decision(false, state);
        acceptedGeneration = generation;
        pendingRequestId = 0L;
        pendingStartedNanos = 0L;
        state = RefreshState.RECEIVED;
        return new Decision(true, state);
    }

    public boolean timeout(long requestId, long nowNanos, long timeoutNanos) {
        if (state != RefreshState.REQUESTING || pendingRequestId != requestId || nowNanos < pendingStartedNanos
                || nowNanos - pendingStartedNanos < timeoutNanos) return false;
        pendingRequestId = 0L;
        pendingStartedNanos = 0L;
        state = RefreshState.TIMED_OUT;
        return true;
    }

    public long epoch() { return epoch; }
    public long pendingRequestId() { return pendingRequestId; }
    public RefreshState state() { return state; }
    /** Counters deliberately use positive values and wrap from MAX_VALUE to 1. */
    public static boolean after(long candidate, long current) {
        return candidate > current || (current == Long.MAX_VALUE && candidate == 1L);
    }
    private static long nextPositive(long value) { return value == Long.MAX_VALUE ? 1L : value + 1L; }
}

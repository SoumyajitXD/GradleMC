package com.soumyajit.gradlemc.foundation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Bounded server-thread capture queue.  A Forge event calls {@link #drainServerQueue()} once at
 * server-tick end; workers wait only on their own request and the server thread never waits.
 */
public final class GameThreadBridge {
    public interface ServerAvailability { boolean available(); boolean onServerThread(); }
    /** Monotonic time is deliberately separate from wall-clock timestamps used in reports. */
    @FunctionalInterface public interface MonotonicClock { long nanoTime(); }
    private final Clock clock;
    private final MonotonicClock monotonicClock;
    private final GameThreadDispatchPolicy policy;
    private final ServerAvailability server;
    private final ArrayDeque<Pending<?>> pending = new ArrayDeque<>();
    private volatile boolean stopping;
    private long deferred;

    public GameThreadBridge(Clock clock, GameThreadDispatchPolicy policy, ServerAvailability server) {
        this(clock, policy, server, System::nanoTime);
    }
    GameThreadBridge(Clock clock, GameThreadDispatchPolicy policy, ServerAvailability server, MonotonicClock monotonicClock) {
        this.clock = Objects.requireNonNull(clock, "clock"); this.policy = Objects.requireNonNull(policy, "policy");
        this.server = Objects.requireNonNull(server, "server");
        this.monotonicClock = Objects.requireNonNull(monotonicClock, "monotonicClock");
    }
    public synchronized int queuedCount() { return pending.size(); }
    public synchronized long deferredCount() { return deferred; }
    public <T> CompletableFuture<GameThreadResult<T>> dispatch(GameThreadRequest<T> request) {
        Objects.requireNonNull(request, "request");
        if (request.target() == GameThreadTarget.WORKER_SAFE || request.target() == GameThreadTarget.CALLER_SAFE) return direct(request, Duration.ZERO);
        if (request.target() == GameThreadTarget.CLIENT_MAIN_THREAD_CAPTURE) return CompletableFuture.completedFuture(result(request, GameThreadBridgeStatus.UNAVAILABLE, null, Duration.ZERO, Duration.ZERO, "Client bridge is unavailable on this runtime."));
        if (stopping) return CompletableFuture.completedFuture(result(request, GameThreadBridgeStatus.STOPPING, null, Duration.ZERO, Duration.ZERO, "Server is stopping."));
        if (!server.available()) return CompletableFuture.completedFuture(result(request, GameThreadBridgeStatus.UNAVAILABLE, null, Duration.ZERO, Duration.ZERO, "Server is unavailable."));
        if (server.onServerThread()) return direct(request, Duration.ZERO);
        synchronized (this) {
            if (stopping) return CompletableFuture.completedFuture(result(request, GameThreadBridgeStatus.STOPPING, null, Duration.ZERO, Duration.ZERO, "Server is stopping."));
            if (pending.size() >= policy.maxQueuedRequests()) return CompletableFuture.completedFuture(result(request, GameThreadBridgeStatus.REJECTED, null, Duration.ZERO, Duration.ZERO, "Game-thread queue is full."));
            Pending<T> value = new Pending<>(request, clock.instant(), monotonicClock.nanoTime()); pending.addLast(value); return value.future;
        }
    }
    public void drainServerQueue() {
        if (stopping || !server.available() || !server.onServerThread()) return;
        long tickStart = monotonicClock.nanoTime(); int started = 0;
        while (started < policy.maxRequestsPerTick() && elapsed(tickStart) < policy.captureBudgetPerTick().toNanos()) {
            Pending<?> value; synchronized (this) { value = pending.pollFirst(); }
            if (value == null) return;
            run(value); started++;
        }
        synchronized (this) { if (!pending.isEmpty()) deferred += pending.size(); }
    }
    public synchronized void stopServer() {
        stopping = true;
        while (!pending.isEmpty()) complete(pending.removeFirst(), GameThreadBridgeStatus.STOPPING, null, Duration.ZERO, "Server stopped before capture.");
    }
    public synchronized void startServer() { stopping = false; }
    private <T> CompletableFuture<GameThreadResult<T>> direct(GameThreadRequest<T> request, Duration queueDelay) {
        CompletableFuture<GameThreadResult<T>> future = new CompletableFuture<>();
        if (request.cancellation().cancelled()) future.complete(result(request, GameThreadBridgeStatus.CANCELLED, null, queueDelay, Duration.ZERO, "Cancelled before capture."));
        else {
            long start = System.nanoTime();
            try { future.complete(result(request, GameThreadBridgeStatus.COMPLETED, request.capture().call(), queueDelay, Duration.ofNanos(System.nanoTime() - start), "Completed.")); }
            catch (Exception exception) { future.complete(result(request, GameThreadBridgeStatus.FAILED, null, queueDelay, Duration.ofNanos(System.nanoTime() - start), "Capture failed: " + exception.getClass().getSimpleName())); }
        }
        return future;
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) private void run(Pending value) {
        GameThreadRequest request = value.request; Duration delay = Duration.ofNanos(elapsed(value.queuedNanos));
        if (request.cancellation().cancelled()) { complete(value, GameThreadBridgeStatus.CANCELLED, null, delay, "Cancelled before capture."); return; }
        if (delay.compareTo(policy.maxRequestAge()) > 0 || delay.compareTo(request.timeout()) > 0) { complete(value, GameThreadBridgeStatus.TIMED_OUT, null, delay, "Capture timed out while queued."); return; }
        long start = System.nanoTime();
        try { complete(value, GameThreadBridgeStatus.COMPLETED, request.capture().call(), delay, Duration.ofNanos(System.nanoTime() - start), "Completed."); }
        catch (Exception exception) { complete(value, GameThreadBridgeStatus.FAILED, null, delay, Duration.ofNanos(System.nanoTime() - start), "Capture failed: " + exception.getClass().getSimpleName()); }
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) private void complete(Pending value, GameThreadBridgeStatus status, Object output, Duration delay, String detail) { complete(value, status, output, delay, Duration.ZERO, detail); }
    @SuppressWarnings({"rawtypes", "unchecked"}) private void complete(Pending value, GameThreadBridgeStatus status, Object output, Duration delay, Duration capture, String detail) { value.future.complete(new GameThreadResult(value.request.id(), status, java.util.Optional.ofNullable(output), delay, capture, detail)); }
    private long elapsed(long startedNanos) {
        long elapsed = monotonicClock.nanoTime() - startedNanos;
        return elapsed < 0L ? 0L : elapsed;
    }
    private static <T> GameThreadResult<T> result(GameThreadRequest<T> request, GameThreadBridgeStatus status, T value, Duration delay, Duration capture, String detail) { return new GameThreadResult<>(request.id(), status, java.util.Optional.ofNullable(value), delay, capture, detail); }
    private static final class Pending<T> { final GameThreadRequest<T> request; final Instant queuedAt; final long queuedNanos; final CompletableFuture<GameThreadResult<T>> future = new CompletableFuture<>(); Pending(GameThreadRequest<T> request, Instant queuedAt, long queuedNanos) { this.request=request; this.queuedAt=queuedAt; this.queuedNanos=queuedNanos; } }
}

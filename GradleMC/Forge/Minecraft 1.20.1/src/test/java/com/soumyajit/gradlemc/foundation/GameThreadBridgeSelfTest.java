package com.soumyajit.gradlemc.foundation;

import java.time.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Deterministic bridge contract checks; no Minecraft classes, sleeps, or real worker threads. */
public final class GameThreadBridgeSelfTest {
    private GameThreadBridgeSelfTest() { }
    public static void run() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        MutableNanos nanos = new MutableNanos();
        FakeServer server = new FakeServer();
        GameThreadBridge bridge = new GameThreadBridge(clock, new GameThreadDispatchPolicy(1, 1, Duration.ofMillis(5), Duration.ofSeconds(2)), server, nanos);
        TaskCore.CancellationToken token = new TaskCore.CancellationToken();
        GameThreadRequest<String> request = request("capture-one", token, () -> "ok");
        check(bridge.dispatch(request).isDone() == false, "off-thread capture queues");
        check(bridge.queuedCount() == 1, "queue count");
        check(bridge.dispatch(request("capture-two", new TaskCore.CancellationToken(), () -> "no")).join().status() == GameThreadBridgeStatus.REJECTED, "queue cap");
        server.serverThread.set(true); bridge.drainServerQueue();
        check(bridge.queuedCount() == 0, "drained");
        check(bridge.dispatch(request("direct", new TaskCore.CancellationToken(), () -> "direct")).join().status() == GameThreadBridgeStatus.COMPLETED, "direct server execution");
        server.serverThread.set(false);
        token.cancel(); bridge.dispatch(request("cancelled", token, () -> "bad")); server.serverThread.set(true); bridge.drainServerQueue();
        server.serverThread.set(false);
        var wallClockShifted = bridge.dispatch(request("wall-clock-shift", new TaskCore.CancellationToken(), () -> "ok"));
        clock.advance(Duration.ofSeconds(3)); server.serverThread.set(true); bridge.drainServerQueue();
        check(wallClockShifted.join().status() == GameThreadBridgeStatus.COMPLETED, "wall clock does not expire queue work");
        server.serverThread.set(false);
        var old = bridge.dispatch(request("old", new TaskCore.CancellationToken(), () -> "bad"));
        nanos.advance(Duration.ofSeconds(3)); server.serverThread.set(true); bridge.drainServerQueue();
        check(old.join().status() == GameThreadBridgeStatus.TIMED_OUT, "monotonic queue age expires work");
        check(bridge.dispatch(request("failure", new TaskCore.CancellationToken(), () -> { throw new IllegalStateException(); })).join().status() == GameThreadBridgeStatus.FAILED, "exception propagation");
        server.serverThread.set(false); bridge.dispatch(request("stopping", new TaskCore.CancellationToken(), () -> "bad")); bridge.stopServer();
        check(bridge.queuedCount() == 0, "stop cleanup");
    }
    private static GameThreadRequest<String> request(String id, TaskCore.CancellationToken token, java.util.concurrent.Callable<String> work) { return new GameThreadRequest<>(id, id, GameThreadTarget.SERVER_MAIN_THREAD_CAPTURE, Duration.ofSeconds(1), token, work); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static final class FakeServer implements GameThreadBridge.ServerAvailability { final AtomicBoolean serverThread = new AtomicBoolean(); @Override public boolean available() { return true; } @Override public boolean onServerThread() { return serverThread.get(); } }
    private static final class MutableNanos implements GameThreadBridge.MonotonicClock { private long value; void advance(Duration duration) { value += duration.toNanos(); } @Override public long nanoTime() { return value; } }
    private static final class MutableClock extends Clock { private Instant now; MutableClock(Instant now) { this.now=now; } void advance(Duration duration) { now=now.plus(duration); } @Override public ZoneId getZone() { return ZoneOffset.UTC; } @Override public Clock withZone(ZoneId zone) { return this; } @Override public Instant instant() { return now; } }
}

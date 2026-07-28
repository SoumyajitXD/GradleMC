package com.soumyajit.gradlemc.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotLifecycleTest {
    @Test void currentResponseIsAccepted() { SnapshotLifecycle l = connected(); var r = l.request(10); assertTrue(l.receive(r.epoch(), r.requestId(), 1).accepted()); }
    @Test void oldRequestCannotClearNewPendingRequest() { SnapshotLifecycle l=connected(); var a=l.request(1); var b=l.request(2); assertFalse(l.receive(a.epoch(),a.requestId(),1).accepted()); assertEquals(b.requestId(),l.pendingRequestId()); }
    @Test void duplicateResponseIsHarmless() { SnapshotLifecycle l=connected(); var r=l.request(1); assertTrue(l.receive(r.epoch(),r.requestId(),1).accepted()); assertFalse(l.receive(r.epoch(),r.requestId(),1).accepted()); }
    @Test void olderGenerationIsIgnored() { SnapshotLifecycle l=connected(); var a=l.request(1); assertTrue(l.receive(a.epoch(),a.requestId(),2).accepted()); var b=l.request(2); assertFalse(l.receive(b.epoch(),b.requestId(),1).accepted()); }
    @Test void newerGenerationIsAccepted() { SnapshotLifecycle l=connected(); var a=l.request(1); l.receive(a.epoch(),a.requestId(),1); var b=l.request(2); assertTrue(l.receive(b.epoch(),b.requestId(),2).accepted()); }
    @Test void previousEpochIsIgnored() { SnapshotLifecycle l=connected(); var r=l.request(1); l.connect(); assertFalse(l.receive(r.epoch(),r.requestId(),1).accepted()); }
    @Test void disconnectInvalidatesPendingRequest() { SnapshotLifecycle l=connected(); l.request(1); l.disconnect(); assertEquals(0,l.pendingRequestId()); assertEquals(SnapshotLifecycle.RefreshState.DISCONNECTED,l.state()); }
    @Test void reconnectUsesNewEpoch() { SnapshotLifecycle l=connected(); long old=l.epoch(); l.disconnect(); l.connect(); assertNotEquals(old,l.epoch()); }
    @Test void timeoutAppliesOnlyToMatchingRequest() { SnapshotLifecycle l=connected(); var a=l.request(1); l.request(2); assertFalse(l.timeout(a.requestId(),10,5)); }
    @Test void responseBeforeTimeoutWins() { SnapshotLifecycle l=connected(); var r=l.request(1); assertTrue(l.receive(r.epoch(),r.requestId(),1).accepted()); assertFalse(l.timeout(r.requestId(),10,5)); }
    @Test void responseAfterTimeoutIsRejected() { SnapshotLifecycle l=connected(); var r=l.request(1); assertTrue(l.timeout(r.requestId(),10,5)); assertFalse(l.receive(r.epoch(),r.requestId(),1).accepted()); }
    @Test void generationWrapIsOrdered() { assertTrue(SnapshotLifecycle.after(1L, Long.MAX_VALUE)); }
    private static SnapshotLifecycle connected() { SnapshotLifecycle l=new SnapshotLifecycle(); l.connect(); return l; }
}

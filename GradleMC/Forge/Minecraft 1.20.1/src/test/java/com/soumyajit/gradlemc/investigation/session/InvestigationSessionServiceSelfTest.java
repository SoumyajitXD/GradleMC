package com.soumyajit.gradlemc.investigation.session;

import com.soumyajit.gradlemc.foundation.*;
import com.soumyajit.gradlemc.investigation.*;
import com.soumyajit.gradlemc.investigation.planning.*;
import com.soumyajit.gradlemc.investigation.storage.InvestigationStore;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Asynchronous coordinator contract checks without Minecraft objects or sleep-based races. */
public final class InvestigationSessionServiceSelfTest {
    private InvestigationSessionServiceSelfTest() { }
    public static void run() throws Exception {
        Path game = Files.createTempDirectory("gradlemc-session-");
        try {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicBoolean requestTokenCancelled = new AtomicBoolean();
            InvestigationSessionService service = new InvestigationSessionService(InvestigationPlanningAdapter.builtins(), new InvestigationStore(game),
                    Clock.systemUTC(), (roots, context) -> { entered.countDown(); if (!release.await(2, TimeUnit.SECONDS)) throw new AssertionError("worker was not released"); requestTokenCancelled.set(context.cancellation().cancelled()); return List.of(); });
            try {
                InvestigationSessionSnapshot accepted = service.start(request());
                check(accepted.status() == InvestigationSessionStatus.PLANNED, "start returns before execution");
                check(entered.await(2, TimeUnit.SECONDS), "owned worker executes");
                bad(() -> service.start(request()));
                check(service.cancel(), "active cancellation accepted");
                release.countDown();
                check(service.awaitIdle(2, TimeUnit.SECONDS), "worker completed after release");
                check(requestTokenCancelled.get(), "only the request-owned Foundation token is cancelled");
                check(service.active().isEmpty(), "terminal completion releases active slot");
                check(new InvestigationStore(game).read(accepted.id()).state() == InvestigationState.CANCELLED, "cancelled state persisted");
            } finally { release.countDown(); service.close(); }
        } finally { try (var paths = Files.walk(game)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
    }
    private static InvestigationSessionRequest request() {
        StaticFingerprint stat = StaticFingerprint.of(1, Map.of("test", "session"));
        TaskCore.Context foundation = new TaskCore.Context(Clock.systemUTC(), new TaskCore.CancellationToken(), stat, RuntimeContextFingerprint.of(1, stat, Map.of()), Map.of());
        return new InvestigationSessionRequest(new InvestigationProfileId("quick-health"), new InvestigationPlanningContext(InvestigationPlanningContext.Side.COMMON, foundation),
                new InvestigationRuntimeIdentity("1.0.3", "1.20.1", "47.4.20", "COMMON"), "test", InvestigationSessionPolicy.SAFE_DEFAULT);
    }
    private static void bad(Runnable action) { try { action.run(); throw new AssertionError("expected rejection"); } catch (IllegalStateException expected) { } }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}

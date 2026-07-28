package com.soumyajit.gradlemc.gametest;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.foundation.GameThreadDispatchPolicy;
import com.soumyajit.gradlemc.foundation.GameThreadBridge;
import com.soumyajit.gradlemc.foundation.GameThreadBridgeStatus;
import com.soumyajit.gradlemc.foundation.GameThreadRequest;
import com.soumyajit.gradlemc.foundation.GameThreadTarget;
import com.soumyajit.gradlemc.foundation.RuntimeContextFingerprint;
import com.soumyajit.gradlemc.foundation.StaticFingerprint;
import com.soumyajit.gradlemc.foundation.TaskCore;
import com.soumyajit.gradlemc.investigation.InvestigationId;
import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import com.soumyajit.gradlemc.investigation.InvestigationRuntimeIdentity;
import com.soumyajit.gradlemc.investigation.planning.InvestigationPlanningAdapter;
import com.soumyajit.gradlemc.investigation.planning.InvestigationPlanningContext;
import com.soumyajit.gradlemc.investigation.profile.InvestigationProfileRegistry;
import com.soumyajit.gradlemc.investigation.session.InvestigationSessionPolicy;
import com.soumyajit.gradlemc.investigation.session.InvestigationSessionRequest;
import com.soumyajit.gradlemc.investigation.session.InvestigationSessionService;
import com.soumyajit.gradlemc.investigation.storage.InvestigationStore;
import com.soumyajit.gradlemc.metrics.ServerHealthTelemetry;
import com.soumyajit.gradlemc.metrics.TickMonitorService;
import com.soumyajit.gradlemc.profiler.GradleMcProfilerService;
import com.soumyajit.gradlemc.profiler.ProfilerSessionConfig;
import com.soumyajit.gradlemc.profiler.tick.SlowTickDetector;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime-discovered Forge tests for release-critical, side-neutral contracts.  They deliberately
 * use Minecraft's stock empty template so the tests exercise real Forge discovery without adding
 * a world structure or depending on a player/client.
 */
@GameTestHolder(GradleMC.MOD_ID)
public final class GradleMcGameTests {
    private GradleMcGameTests() { }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void bridge_policy_is_bounded(GameTestHelper helper) {
        GameThreadDispatchPolicy policy = GameThreadDispatchPolicy.SAFE_DEFAULT;
        require(policy.maxQueuedRequests() > 0 && policy.maxQueuedRequests() <= 64 && policy.maxRequestsPerTick() > 0, "bridge policy is not bounded");
        require(GameThreadTarget.SERVER_MAIN_THREAD_CAPTURE != GameThreadTarget.CLIENT_MAIN_THREAD_CAPTURE, "server and client targets must remain distinct");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void bridge_direct_and_queued_dispatch(GameTestHelper helper) {
        TestServer server = new TestServer();
        GameThreadBridge bridge = new GameThreadBridge(Clock.systemUTC(), new GameThreadDispatchPolicy(4, 4, Duration.ofMillis(5), Duration.ofSeconds(1)), server);
        server.serverThread.set(true);
        require(bridge.dispatch(request("bridge-direct", () -> "direct")).join().status() == GameThreadBridgeStatus.COMPLETED, "direct server dispatch failed");
        server.serverThread.set(false);
        var queued = bridge.dispatch(request("bridge-queued", () -> "queued"));
        require(!queued.isDone() && bridge.queuedCount() == 1, "off-thread dispatch did not queue");
        server.serverThread.set(true);
        bridge.drainServerQueue();
        require(queued.join().status() == GameThreadBridgeStatus.COMPLETED, "queued server dispatch failed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void bridge_stop_cancels_queued_capture(GameTestHelper helper) {
        TestServer server = new TestServer();
        GameThreadBridge bridge = new GameThreadBridge(Clock.systemUTC(), GameThreadDispatchPolicy.SAFE_DEFAULT, server);
        var queued = bridge.dispatch(request("bridge-stop", () -> "never"));
        bridge.stopServer();
        require(queued.join().status() == GameThreadBridgeStatus.STOPPING && bridge.queuedCount() == 0, "bridge stop leaked queued capture");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void investigation_ids_reject_paths(GameTestHelper helper) {
        require(new InvestigationId("inv-runtime-test").value().startsWith("inv-"), "safe ID rejected");
        boolean rejected = false;
        try { new InvestigationId("../escape"); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "path-like ID accepted");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void built_in_profiles_are_fixed_and_lowercase(GameTestHelper helper) {
        var profiles = InvestigationProfileRegistry.builtins().all();
        require(profiles.size() == 4, "unexpected built-in profile count");
        for (var profile : profiles) require(profile.id().value().equals(profile.id().value().toLowerCase(java.util.Locale.ROOT)), "mixed-case profile ID");
        require(InvestigationProfileRegistry.builtins().find(new InvestigationProfileId("quick-health")).isPresent(), "quick-health unavailable");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void investigation_plan_is_side_effect_free(GameTestHelper helper) {
        var context = planningContext();
        var plan = InvestigationPlanningAdapter.builtins().plan(new InvestigationProfileId("quick-health"), context);
        require(plan.tasks().size() > 0 && plan.profileId().value().equals("quick-health"), "fixed profile did not create a plan");
        require(!plan.tasks().stream().anyMatch(task -> task.taskId().value().contains("..")), "plan exposed an unsafe task ID");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void investigation_async_start_has_one_active_guard(GameTestHelper helper) {
        Path root;
        try { root = Files.createTempDirectory("gradlemc-gametest-session-"); }
        catch (Exception exception) { throw new AssertionError("temporary Investigation root unavailable", exception); }
        CountDownLatch release = new CountDownLatch(1);
        InvestigationSessionService service = new InvestigationSessionService(InvestigationPlanningAdapter.builtins(), new InvestigationStore(root), Clock.systemUTC(),
                (roots, context) -> { if (!release.await(1, TimeUnit.SECONDS)) throw new IllegalStateException("test worker release timed out"); return List.of(); });
        try {
            var first = service.start(sessionRequest());
            boolean rejected = false;
            try { service.start(sessionRequest()); } catch (IllegalStateException expected) { rejected = true; }
            require(first.status().name().equals("PLANNED") && rejected, "async start did not retain its one-active guard");
            require(service.cancel(), "active investigation did not accept cancellation");
            release.countDown();
            require(service.awaitIdle(1, TimeUnit.SECONDS) && service.active().isEmpty(), "cancelled Investigation did not release its active slot");
        } catch (Exception exception) { throw new AssertionError("async Investigation lifecycle failed", exception); }
        finally { release.countDown(); service.close(); deleteTree(root); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void health_telemetry_accepts_monotonic_ticks(GameTestHelper helper) {
        ServerHealthTelemetry telemetry = new ServerHealthTelemetry();
        telemetry.recordTick(1_000_000_000L, 50_000_000L);
        telemetry.recordTick(1_050_000_000L, 50_000_000L);
        var window = telemetry.snapshot(1_060_000_000L, Instant.EPOCH).windows().get(0);
        require(window.samples() == 2 && window.tps() <= 20.0D, "health telemetry produced invalid tick window");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void health_telemetry_rejects_clock_regression(GameTestHelper helper) {
        ServerHealthTelemetry telemetry = new ServerHealthTelemetry();
        telemetry.recordTick(1_000_000_000L, 50_000_000L);
        telemetry.recordTick(999_000_000L, 50_000_000L);
        require(telemetry.snapshot(1_000_000_000L, Instant.EPOCH).clockAnomalies() == 1, "clock anomaly was not recorded");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void slow_tick_threshold_is_never_below_50ms(GameTestHelper helper) {
        SlowTickDetector detector = new SlowTickDetector(1.0D);
        require(detector.thresholdMillis() == 50.0D && detector.isSlow(50.0D), "slow-tick floor changed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void tick_monitor_triggers_after_warmup_and_hysteresis(GameTestHelper helper) {
        TickMonitorService.stop();
        require(TickMonitorService.start(100.0D, 0.0D), "tick monitor did not start");
        for (int index = 0; index < TickMonitorService.WARMUP_TICKS; index++) TickMonitorService.onTick(200_000_000L);
        TickMonitorService.onTick(200_000_000L);
        TickMonitorService.onTick(200_000_000L);
        require(TickMonitorService.snapshot().incidents() == 1, "tick monitor did not record its bounded incident");
        TickMonitorService.stop();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void profiler_options_are_bounded(GameTestHelper helper) {
        ProfilerSessionConfig config = GradleMcProfilerService.parseOptions("--timeout 99999 --interval 1 --thread *");
        require(config.timeoutSeconds() == ProfilerSessionConfig.MAX_TIMEOUT_SECONDS
                && config.intervalMillis() == ProfilerSessionConfig.MIN_INTERVAL_MILLIS, "profiler options escaped bounds");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", required = true)
    public static void common_mod_identity_has_no_client_requirement(GameTestHelper helper) {
        require("gradlemc".equals(GradleMC.MOD_ID) && "Forge".equals(GradleMC.CURRENT_LOADER_NAME), "common mod identity changed");
        helper.succeed();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static GameThreadRequest<String> request(String id, java.util.concurrent.Callable<String> action) {
        return new GameThreadRequest<>(id, id, GameThreadTarget.SERVER_MAIN_THREAD_CAPTURE, Duration.ofSeconds(1), new TaskCore.CancellationToken(), action);
    }

    private static InvestigationPlanningContext planningContext() {
        StaticFingerprint staticFingerprint = StaticFingerprint.of(1, Map.of("game-test", "gradlemc"));
        return new InvestigationPlanningContext(InvestigationPlanningContext.Side.COMMON,
                new TaskCore.Context(Clock.systemUTC(), new TaskCore.CancellationToken(), staticFingerprint,
                        RuntimeContextFingerprint.of(1, staticFingerprint, Map.of()), Map.of()));
    }

    private static InvestigationSessionRequest sessionRequest() {
        return new InvestigationSessionRequest(new InvestigationProfileId("quick-health"), planningContext(),
                new InvestigationRuntimeIdentity(GradleMC.CURRENT_VERSION, "1.20.1", "47.4.20", "COMMON"), "game-test", InvestigationSessionPolicy.SAFE_DEFAULT);
    }

    private static void deleteTree(Path root) {
        try (var paths = Files.walk(root)) { for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        catch (Exception ignored) { }
    }

    private static final class TestServer implements GameThreadBridge.ServerAvailability {
        final AtomicBoolean serverThread = new AtomicBoolean();
        @Override public boolean available() { return true; }
        @Override public boolean onServerThread() { return serverThread.get(); }
    }
}

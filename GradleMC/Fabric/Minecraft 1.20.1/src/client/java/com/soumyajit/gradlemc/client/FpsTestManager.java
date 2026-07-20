package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.client.overlay.FpsRollingStatsCalculator;
import com.soumyajit.gradlemc.client.overlay.FpsSamplingService;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.metrics.FpsTestResult;
import com.soumyajit.gradlemc.report.FpsTestReportWriter;
import com.soumyajit.gradlemc.smart.AdaptiveBaselineStore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Client-side facade for the single authoritative rendered-frame sampler. */
public final class FpsTestManager {
    private static final FpsSamplingService SAMPLER =
            new FpsSamplingService(GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get());
    private static FpsTestResult latestResult;
    private static Path latestReportPath;
    private static int configuredWindowSeconds = -1;

    private FpsTestManager() {
    }

    public static boolean startFromClient(int seconds) {
        if (seconds < 5 || seconds > maxDurationSeconds()) {
            sendClientMessage(Component.literal("FPS test duration must be between 5 and " + maxDurationSeconds() + " seconds."));
            return false;
        }
        if (!SAMPLER.startTest(seconds, Instant.now())) {
            sendClientMessage(Component.literal("An FPS test is already running. Stop it before starting another."));
            return false;
        }
        sendClientMessage(Component.literal("FPS test started for " + seconds + " active gameplay seconds."));
        return true;
    }

    public static boolean stopFromClient() {
        return SAMPLER.stopTest(Instant.now()).map(result -> {
            publish(result);
            return true;
        }).orElseGet(() -> {
            sendClientMessage(Component.literal("No FPS test is currently running."));
            return false;
        });
    }

    public static boolean isRunning() {
        return SAMPLER.isTestRunning();
    }

    public static Optional<FpsTestResult> latestResult() {
        return Optional.ofNullable(latestResult);
    }

    public static Optional<Path> latestReportPath() {
        return Optional.ofNullable(latestReportPath);
    }

    public static DiagnosticTestProgress progress() {
        return SAMPLER.progress();
    }

    public static FpsRollingStatsCalculator.Snapshot latestSnapshot() {
        return SAMPLER.snapshot(false);
    }

    public static FpsRollingStatsCalculator.Snapshot latestSnapshot(boolean refreshPercentiles) {
        return SAMPLER.snapshot(refreshPercentiles);
    }

    /** Cancels only on world loss. Menu, pause, focus and loading transitions merely reset the pending interval. */
    public static void onClientTick(boolean inWorld) {
        if (!inWorld) {
            SAMPLER.cancelTest(Instant.now()).ifPresent(FpsTestManager::publish);
            SAMPLER.clearRollingStatistics();
        }
    }

    public static void pause() {
        SAMPLER.pause();
    }

    /** A world/dimension replacement ends an active test and prevents prior samples leaking into the next world. */
    public static void onWorldChanged() {
        SAMPLER.cancelTest(Instant.now()).ifPresent(FpsTestManager::publish);
        SAMPLER.clearRollingStatistics();
    }

    /** Invoked exactly once from Fabric's HUD render callback for every active gameplay frame. */
    public static void onRenderedFrame(long nowNanos) {
        if (!shouldSample()) return;
        try {
            int configured = GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get();
            if (configured != configuredWindowSeconds) {
                SAMPLER.setRollingWindowSeconds(configured);
                configuredWindowSeconds = configured;
            }
            SAMPLER.onRenderedFrame(nowNanos).ifPresent(FpsTestManager::publish);
        } catch (RuntimeException exception) {
            SAMPLER.failTest(Instant.now()).ifPresent(FpsTestManager::publish);
            sendClientMessage(Component.literal("FPS sampler failed: " + safeMessage(exception)));
        }
    }

    private static boolean shouldSample() {
        return SAMPLER.isTestRunning() || (GradleMCConfig.OVERLAY_ENABLED.get()
                && (GradleMCConfig.OVERLAY_SHOW_FPS.get() || GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get()
                || GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get() || GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()));
    }

    private static int maxDurationSeconds() {
        return Math.max(5, Math.min(1800, GradleMCConfig.MAX_FPS_TEST_SECONDS.get()));
    }

    private static void publish(FpsTestResult result) {
        latestResult = result;
        if (result.hasSamples()) {
            AdaptiveBaselineStore.recordFps(result);
        }
        try {
            latestReportPath = new FpsTestReportWriter().write(result, GradleMcPaths.reportDirectory());
            sendClientMessage(GradleMcPaths.pathComponent(summary(result) + " Report: ", latestReportPath));
        } catch (IOException exception) {
            latestReportPath = null;
            sendClientMessage(Component.literal(summary(result) + " Report export failed: " + safeMessage(exception)));
        }
    }

    private static String summary(FpsTestResult result) {
        if (!result.hasSamples()) {
            return "FPS test " + result.endReason().name().toLowerCase(Locale.ROOT) + ": no valid rendered-frame samples were collected.";
        }
        return "FPS test " + result.endReason().name().toLowerCase(Locale.ROOT) + ": avg "
                + String.format(Locale.ROOT, "%.1f", result.averageFps()) + " FPS, min " + result.minFps()
                + ", max " + result.maxFps() + ", 1% low " + result.onePercentLowFps().stream()
                .mapToObj(value -> String.format(Locale.ROOT, "%.0f", value)).findFirst().orElse("unavailable") + ".";
    }

    private static void sendClientMessage(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, false);
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}

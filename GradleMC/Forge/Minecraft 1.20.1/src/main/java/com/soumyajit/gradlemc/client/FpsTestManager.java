package com.soumyajit.gradlemc.client;

import com.mojang.brigadier.Command;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.metrics.FpsTestResult;
import com.soumyajit.gradlemc.metrics.FrameTimeStatistics;
import com.soumyajit.gradlemc.report.FpsTestReportWriter;
import com.soumyajit.gradlemc.smart.AdaptiveBaselineStore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

/** A bounded test session fed by the same completed rendered-frame observations as the overlay. */
public final class FpsTestManager {
    private static final long MAX_VALID_FRAME_TIME_NANOS = FrameTimeStatistics.NANOS_PER_SECOND;
    private static Session currentSession;
    private static FpsTestResult latestResult;
    private static Path latestReportPath;

    private FpsTestManager() {
    }

    public static int start(CommandSourceStack source, int seconds) {
        if (currentSession != null) {
            source.sendFailure(Component.literal("An FPS test is already running. Use /gradlemc testfps stop first."));
            return 0;
        }
        currentSession = new Session(seconds, Instant.now());
        source.sendSuccess(() -> Component.literal("FPS test started for " + seconds + " active gameplay seconds."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int stop(CommandSourceStack source) {
        if (currentSession == null) {
            source.sendFailure(Component.literal("No FPS test is currently running."));
            return 0;
        }
        finish(FpsTestResult.EndReason.STOPPED);
        return Command.SINGLE_SUCCESS;
    }

    public static boolean stopFromClient() {
        if (currentSession == null) {
            sendClientMessage(Component.literal("No FPS test is currently running."));
            return false;
        }
        finish(FpsTestResult.EndReason.STOPPED);
        return true;
    }

    public static boolean startFromClient(int seconds) {
        if (currentSession != null) {
            sendClientMessage(Component.literal("An FPS test is already running. Use the stop button or /gradlemc testfps stop first."));
            return false;
        }
        currentSession = new Session(seconds, Instant.now());
        sendClientMessage(Component.literal("FPS test started for " + seconds + " active gameplay seconds."));
        return true;
    }

    public static boolean isRunning() {
        return currentSession != null;
    }

    public static FpsTestResult latestResult() {
        return latestResult;
    }

    public static Path latestReportPath() {
        return latestReportPath;
    }

    public static DiagnosticTestProgress progress() {
        Session session = currentSession;
        return session == null ? DiagnosticTestProgress.idle()
                : new DiagnosticTestProgress(true, session.requestedSeconds, (int) (session.activeElapsedNanos / FrameTimeStatistics.NANOS_PER_SECOND));
    }

    /** Completes a requested active-gameplay duration; paused or unfocused time is deliberately excluded. */
    public static void onClientTick(boolean inWorld) {
        Session session = currentSession;
        if (session == null) {
            return;
        }
        if (!inWorld) {
            finish(FpsTestResult.EndReason.CANCELLED);
        } else if (session.activeElapsedNanos >= session.requestedSeconds * FrameTimeStatistics.NANOS_PER_SECOND) {
            finish(FpsTestResult.EndReason.COMPLETED);
        }
    }

    /** Drops the pending timestamp, so a pause/focus loss is never reported as a slow frame. */
    public static void pause() {
        Session session = currentSession;
        if (session != null) {
            session.pause();
        }
    }

    /** Called once per active post-GUI render callback, sharing the overlay's measurement source. */
    public static void onRenderedFrame(long nowNanos) {
        Session session = currentSession;
        if (session == null) {
            return;
        }
        try {
            session.addFrame(nowNanos);
        } catch (RuntimeException exception) {
            sendClientMessage(Component.literal("FPS test failed: " + safeMessage(exception)));
            finish(FpsTestResult.EndReason.ERROR);
        }
    }

    private static void finish(FpsTestResult.EndReason endReason) {
        Session session = currentSession;
        if (session == null) {
            return;
        }
        currentSession = null;
        FpsTestResult result = session.toResult(endReason, Instant.now());
        latestResult = result;
        AdaptiveBaselineStore.recordFps(result);
        try {
            latestReportPath = new FpsTestReportWriter().write(result, GradleMcPaths.reportDirectory());
            sendClientMessage(GradleMcPaths.pathComponent(summary(result) + " Report: ", latestReportPath));
        } catch (IOException exception) {
            latestReportPath = null;
            sendClientMessage(Component.literal("FPS test complete, but report export failed: " + safeMessage(exception)));
            sendClientMessage(Component.literal(summary(result)));
        }
    }

    private static void sendClientMessage(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, false);
        }
    }

    private static String summary(FpsTestResult result) {
        return "FPS test complete: avg " + String.format(Locale.ROOT, "%.1f", result.averageFps())
                + " FPS, min " + result.minFps() + ", max " + result.maxFps() + ", 1% low "
                + result.onePercentLowFps().stream().mapToObj(value -> String.format(Locale.ROOT, "%.0f", value)).findFirst().orElse("n/a") + ".";
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static final class Session {
        private final int requestedSeconds;
        private final Instant startedAt;
        private final long[] frameTimesNanos;
        private long activeElapsedNanos;
        private long lastFrameNanos = -1L;
        private long observedFrames;
        private long minFrameTimeNanos = Long.MAX_VALUE;
        private long maxFrameTimeNanos;
        private int sampleCount;

        private Session(int requestedSeconds, Instant startedAt) {
            this.requestedSeconds = requestedSeconds;
            this.startedAt = startedAt;
            this.frameTimesNanos = new long[Math.max(120, Math.min(144_000, requestedSeconds * 240))];
        }

        private void pause() {
            lastFrameNanos = -1L;
        }

        private void addFrame(long nowNanos) {
            if (nowNanos <= 0L) {
                pause();
                return;
            }
            if (lastFrameNanos < 0L) {
                lastFrameNanos = nowNanos;
                return;
            }
            long frameTime = nowNanos - lastFrameNanos;
            lastFrameNanos = nowNanos;
            if (frameTime <= 0L || frameTime > MAX_VALID_FRAME_TIME_NANOS) {
                return;
            }
            activeElapsedNanos += frameTime;
            observedFrames++;
            minFrameTimeNanos = Math.min(minFrameTimeNanos, frameTime);
            maxFrameTimeNanos = Math.max(maxFrameTimeNanos, frameTime);
            if (sampleCount < frameTimesNanos.length) {
                frameTimesNanos[sampleCount++] = frameTime;
            }
        }

        private FpsTestResult toResult(FpsTestResult.EndReason endReason, Instant endedAt) {
            double elapsedSeconds = activeElapsedNanos / (double) FrameTimeStatistics.NANOS_PER_SECOND;
            double average = activeElapsedNanos <= 0L ? 0.0D : observedFrames / elapsedSeconds;
            int minFps = observedFrames == 0 ? 0 : (int) Math.round(FrameTimeStatistics.fpsForFrameTime(maxFrameTimeNanos));
            int maxFps = observedFrames == 0 ? 0 : (int) Math.round(FrameTimeStatistics.fpsForFrameTime(minFrameTimeNanos));
            return new FpsTestResult(requestedSeconds, elapsedSeconds, sampleCount, average, minFps, maxFps,
                    sampleCount == 0 ? java.util.OptionalDouble.empty()
                            : java.util.OptionalDouble.of(FrameTimeStatistics.lowFps(frameTimesNanos, sampleCount, 0.01D)),
                    startedAt, endedAt, endReason);
        }
    }
}

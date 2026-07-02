package com.soumyajit.gradlemc.client;

import com.mojang.brigadier.Command;
import com.soumyajit.gradlemc.metrics.FpsTestResult;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.report.FpsTestReportWriter;
import com.soumyajit.gradlemc.smart.AdaptiveBaselineStore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

public final class FpsTestManager {
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
        source.sendSuccess(() -> Component.literal("FPS test started for " + seconds + " seconds."), false);
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
        sendClientMessage(Component.literal("FPS test started for " + seconds + " seconds."));
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
        if (session == null) {
            return DiagnosticTestProgress.idle();
        }
        int elapsed = (int) Duration.between(session.startedAt, Instant.now()).getSeconds();
        return new DiagnosticTestProgress(true, session.requestedSeconds, elapsed);
    }

    public static void onClientTick() {
        Session session = currentSession;
        if (session == null) {
            return;
        }

        try {
            session.addSample(Minecraft.getInstance().getFps());
            if (Duration.between(session.startedAt, Instant.now()).getSeconds() >= session.requestedSeconds) {
                finish(FpsTestResult.EndReason.COMPLETED);
            }
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
                + " FPS, min " + result.minFps()
                + ", max " + result.maxFps()
                + ", 1% low " + result.onePercentLowFps()
                .stream()
                .mapToObj(value -> String.format(Locale.ROOT, "%.0f", value))
                .findFirst()
                .orElse("n/a") + ".";
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static final class Session {
        private final int requestedSeconds;
        private final Instant startedAt;
        private final int maxSamples;
        private final List<Integer> samples;
        private int minFps = Integer.MAX_VALUE;
        private int maxFps = Integer.MIN_VALUE;
        private long fpsTotal;

        private Session(int requestedSeconds, Instant startedAt) {
            this.requestedSeconds = requestedSeconds;
            this.startedAt = startedAt;
            this.maxSamples = requestedSeconds * 20;
            this.samples = new ArrayList<>(maxSamples);
        }

        private void addSample(int fps) {
            int boundedFps = Math.max(0, fps);
            if (samples.size() >= maxSamples) {
                return;
            }
            samples.add(boundedFps);
            fpsTotal += boundedFps;
            minFps = Math.min(minFps, boundedFps);
            maxFps = Math.max(maxFps, boundedFps);
        }

        private FpsTestResult toResult(FpsTestResult.EndReason endReason, Instant endedAt) {
            int sampleCount = samples.size();
            double average = sampleCount == 0 ? 0.0D : (double) fpsTotal / sampleCount;
            return new FpsTestResult(
                    requestedSeconds,
                    Math.max(0.0D, Duration.between(startedAt, endedAt).toMillis() / 1000.0D),
                    sampleCount,
                    average,
                    sampleCount == 0 ? 0 : minFps,
                    sampleCount == 0 ? 0 : maxFps,
                    onePercentLow(),
                    startedAt,
                    endedAt,
                    endReason
            );
        }

        private OptionalDouble onePercentLow() {
            int sampleCount = samples.size();
            if (sampleCount == 0) {
                return OptionalDouble.empty();
            }
            int lowSampleCount = Math.max(1, (int) Math.ceil(sampleCount * 0.01D));
            return samples.stream()
                    .sorted(Comparator.naturalOrder())
                    .limit(lowSampleCount)
                    .mapToInt(Integer::intValue)
                    .average();
        }
    }
}

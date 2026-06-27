package com.soumyajit.gradlemc.metrics;

import com.mojang.brigadier.Command;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.report.PerformanceTestReportWriter;
import com.soumyajit.gradlemc.smart.AdaptiveBaselineStore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.StreamSupport;

public final class PerformanceTestManager {
    private static Session currentSession;
    private static PerformanceTestResult latestResult;
    private static Path latestReportPath;

    private PerformanceTestManager() {
    }

    public static int start(CommandSourceStack source, int seconds) {
        if (currentSession != null) {
            source.sendFailure(Component.literal("A performance test is already running. Use /gradlemc perf stop first."));
            return 0;
        }
        UUID requester = source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
        currentSession = new Session(seconds, Instant.now(), source.getServer(), requester);
        source.sendSuccess(() -> Component.literal("Performance test started for " + seconds + " seconds."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int stop(CommandSourceStack source) {
        if (currentSession == null) {
            source.sendFailure(Component.literal("No performance test is currently running."));
            return 0;
        }
        finish(source.getServer(), PerformanceTestResult.EndReason.STOPPED, source);
        return Command.SINGLE_SUCCESS;
    }

    public static boolean isRunning() {
        return currentSession != null;
    }

    public static PerformanceTestResult latestResult() {
        return latestResult;
    }

    public static Path latestReportPath() {
        return latestReportPath;
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || currentSession == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        try {
            Session session = currentSession;
            session.sample(server);
            if (Duration.between(session.startedAt, Instant.now()).getSeconds() >= session.requestedSeconds) {
                finish(server, PerformanceTestResult.EndReason.COMPLETED, null);
            }
        } catch (RuntimeException exception) {
            GradleMC.LOGGER.warn("GradleMC performance test failed", exception);
            finish(server, PerformanceTestResult.EndReason.ERROR, null);
        }
    }

    private static void finish(MinecraftServer server, PerformanceTestResult.EndReason endReason, CommandSourceStack source) {
        Session session = currentSession;
        if (session == null) {
            return;
        }
        currentSession = null;
        PerformanceTestResult result = session.toResult(server, endReason, Instant.now());
        latestResult = result;
        AdaptiveBaselineStore.recordPerformance(result);
        try {
            latestReportPath = new PerformanceTestReportWriter().write(result, GradleMcPaths.reportDirectory());
            sendCompletion(server, session.requester, source, "Performance test complete: approx "
                    + format(result.approximateTps()) + " TPS, avg "
                    + format(result.averageTickMs()) + " MSPT. Report: " + latestReportPath);
        } catch (IOException exception) {
            latestReportPath = null;
            sendCompletion(server, session.requester, source, "Performance test complete, but report export failed: "
                    + safeMessage(exception));
        }
    }

    private static void sendCompletion(MinecraftServer server, UUID requester, CommandSourceStack source, String message) {
        if (source != null) {
            source.sendSuccess(() -> Component.literal(message), false);
            return;
        }
        if (requester != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(requester);
            if (player != null) {
                player.sendSystemMessage(Component.literal(message));
                return;
            }
        }
        GradleMC.LOGGER.info(message);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static final class Session {
        private final int requestedSeconds;
        private final Instant startedAt;
        private final UUID requester;
        private final long memoryStartMiB;
        private final int playersStart;
        private final int worldsStart;
        private long previousTickNanos;
        private int sampleCount;
        private double tickTotalMs;
        private double minTickMs = Double.MAX_VALUE;
        private double maxTickMs;
        private double intervalTotalMs;
        private double minIntervalMs = Double.MAX_VALUE;
        private double maxIntervalMs;

        private Session(int requestedSeconds, Instant startedAt, MinecraftServer server, UUID requester) {
            this.requestedSeconds = requestedSeconds;
            this.startedAt = startedAt;
            this.requester = requester;
            this.memoryStartMiB = RuntimeSnapshots.memory().usedMiB();
            this.playersStart = server.getPlayerCount();
            this.worldsStart = worldCount(server);
        }

        private void sample(MinecraftServer server) {
            long now = System.nanoTime();
            double tickMs = Math.max(0.0D, server.getAverageTickTime());
            sampleCount++;
            tickTotalMs += tickMs;
            minTickMs = Math.min(minTickMs, tickMs);
            maxTickMs = Math.max(maxTickMs, tickMs);

            if (previousTickNanos > 0L) {
                double intervalMs = (now - previousTickNanos) / 1_000_000.0D;
                intervalTotalMs += intervalMs;
                minIntervalMs = Math.min(minIntervalMs, intervalMs);
                maxIntervalMs = Math.max(maxIntervalMs, intervalMs);
            }
            previousTickNanos = now;
        }

        private PerformanceTestResult toResult(MinecraftServer server, PerformanceTestResult.EndReason endReason, Instant endedAt) {
            int intervalSamples = Math.max(0, sampleCount - 1);
            double averageTickMs = sampleCount == 0 ? 0.0D : tickTotalMs / sampleCount;
            double averageIntervalMs = intervalSamples == 0 ? 50.0D : intervalTotalMs / intervalSamples;
            double approximateTps = averageIntervalMs <= 0.0D ? 20.0D : Math.min(20.0D, 1000.0D / averageIntervalMs);
            return new PerformanceTestResult(
                    requestedSeconds,
                    Math.max(0.0D, Duration.between(startedAt, endedAt).toMillis() / 1000.0D),
                    sampleCount,
                    approximateTps,
                    averageTickMs,
                    sampleCount == 0 ? 0.0D : minTickMs,
                    sampleCount == 0 ? 0.0D : maxTickMs,
                    intervalSamples == 0 ? 0.0D : minIntervalMs,
                    intervalSamples == 0 ? 0.0D : maxIntervalMs,
                    memoryStartMiB,
                    RuntimeSnapshots.memory().usedMiB(),
                    playersStart,
                    server.getPlayerCount(),
                    worldsStart,
                    worldCount(server),
                    startedAt,
                    endedAt,
                    endReason
            );
        }
    }

    private static int worldCount(MinecraftServer server) {
        return (int) StreamSupport.stream(server.getAllLevels().spliterator(), false).count();
    }
}

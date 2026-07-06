package com.soumyajit.gradlemc.metrics;

import com.mojang.brigadier.Command;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.report.WorldgenObservationReportWriter;
import com.soumyajit.gradlemc.smart.AdaptiveBaselineStore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class WorldgenObservationManager {
    private static final int SAMPLE_EVERY_TICKS = 20;
    private static final int MAX_PLAYER_SNAPSHOTS = 24;
    private static Session currentSession;
    private static WorldgenObservationResult latestResult;
    private static Path latestReportPath;

    private WorldgenObservationManager() {
    }

    public static int start(CommandSourceStack source, int seconds) {
        if (currentSession != null) {
            source.sendFailure(Component.literal("A worldgen observation is already running. Use /gradlemc worldgen stop first."));
            return 0;
        }
        UUID requester = source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
        currentSession = new Session(seconds, Instant.now(), source.getServer(), requester);
        source.sendSuccess(() -> Component.literal("Worldgen observation started for " + seconds
                + " seconds. Move naturally; GradleMC will not generate chunks."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int stop(CommandSourceStack source) {
        if (currentSession == null) {
            source.sendFailure(Component.literal("No worldgen observation is currently running."));
            return 0;
        }
        finish(source.getServer(), WorldgenObservationResult.EndReason.STOPPED, source);
        return Command.SINGLE_SUCCESS;
    }

    public static int status(CommandSourceStack source) {
        Session session = currentSession;
        if (session == null) {
            source.sendSuccess(() -> Component.literal("No worldgen observation is currently running."), false);
            return 0;
        }
        long elapsed = Duration.between(session.startedAt, Instant.now()).getSeconds();
        source.sendSuccess(() -> Component.literal("Worldgen observation running: " + elapsed + "/"
                + session.requestedSeconds + " seconds, samples " + session.sampleCount + "."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int latest(CommandSourceStack source) {
        if (latestResult == null) {
            source.sendSuccess(() -> Component.literal("No worldgen observation report has been created yet."), false);
            return 0;
        }
        WorldgenObservationResult result = latestResult;
        source.sendSuccess(() -> Component.literal("Latest worldgen observation: "
                + format(result.actualSeconds()) + "s, chunks "
                + result.loadedChunksStart() + " -> " + result.loadedChunksEnd()
                + ", max MSPT " + format(result.maxTickMs())
                + (latestReportPath == null ? "" : ", report: " + GradleMcPaths.displayPath(latestReportPath))), false);
        return Command.SINGLE_SUCCESS;
    }

    public static WorldgenObservationResult latestResult() {
        return latestResult;
    }

    public static Path latestReportPath() {
        return latestReportPath;
    }

    public static boolean isRunning() {
        return currentSession != null;
    }

    public static DiagnosticTestProgress progress() {
        Session session = currentSession;
        if (session == null) {
            return DiagnosticTestProgress.idle();
        }
        int elapsed = (int) Duration.between(session.startedAt, Instant.now()).getSeconds();
        return new DiagnosticTestProgress(true, session.requestedSeconds, elapsed);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (currentSession == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        try {
            Session session = currentSession;
            session.onTick(server);
            if (Duration.between(session.startedAt, Instant.now()).getSeconds() >= session.requestedSeconds) {
                finish(server, WorldgenObservationResult.EndReason.COMPLETED, null);
            }
        } catch (RuntimeException exception) {
            GradleMC.LOGGER.warn("GradleMC worldgen observation failed", exception);
            finish(server, WorldgenObservationResult.EndReason.ERROR, null);
        }
    }

    private static void finish(MinecraftServer server, WorldgenObservationResult.EndReason endReason, CommandSourceStack source) {
        Session session = currentSession;
        if (session == null) {
            return;
        }
        currentSession = null;
        WorldgenObservationResult result = session.toResult(server, endReason, Instant.now());
        latestResult = result;
        AdaptiveBaselineStore.recordWorldgen(result);
        try {
            latestReportPath = new WorldgenObservationReportWriter().write(result, GradleMcPaths.reportDirectory());
            sendCompletion(server, session.requester, source, GradleMcPaths.pathComponent("Worldgen observation complete: chunks "
                    + result.loadedChunksStart() + " -> " + result.loadedChunksEnd()
                    + ", max " + format(result.maxTickMs()) + " MSPT. Report: ", latestReportPath));
        } catch (IOException exception) {
            latestReportPath = null;
            sendCompletion(server, session.requester, source, Component.literal("Worldgen observation complete, but report export failed: "
                    + safeMessage(exception)));
        }
    }

    private static void sendCompletion(MinecraftServer server, UUID requester, CommandSourceStack source, Component message) {
        if (source != null) {
            source.sendSuccess(() -> message, false);
            return;
        }
        if (requester != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(requester);
            if (player != null) {
                player.sendSystemMessage(message);
                return;
            }
        }
        GradleMC.LOGGER.info(message.getString());
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
        private final int loadedChunksStart;
        private final Set<String> dimensions = new LinkedHashSet<>();
        private final List<String> playerSnapshots = new ArrayList<>();
        private int tickCounter;
        private int sampleCount;
        private int loadedChunksMin;
        private int loadedChunksMax;
        private double tickTotalMs;
        private double maxTickMs;

        private Session(int requestedSeconds, Instant startedAt, MinecraftServer server, UUID requester) {
            this.requestedSeconds = requestedSeconds;
            this.startedAt = startedAt;
            this.requester = requester;
            this.memoryStartMiB = RuntimeSnapshots.memory().usedMiB();
            this.loadedChunksStart = loadedChunkCount(server);
            this.loadedChunksMin = loadedChunksStart;
            this.loadedChunksMax = loadedChunksStart;
            sample(server);
        }

        private void onTick(MinecraftServer server) {
            tickCounter++;
            if (tickCounter % SAMPLE_EVERY_TICKS == 0) {
                sample(server);
            }
        }

        private void sample(MinecraftServer server) {
            int loadedChunks = loadedChunkCount(server);
            double tickMs = Math.max(0.0D, (server.getAverageTickTimeNanos() / 1_000_000.0D));
            sampleCount++;
            loadedChunksMin = Math.min(loadedChunksMin, loadedChunks);
            loadedChunksMax = Math.max(loadedChunksMax, loadedChunks);
            tickTotalMs += tickMs;
            maxTickMs = Math.max(maxTickMs, tickMs);
            for (ServerLevel level : server.getAllLevels()) {
                dimensions.add(level.dimension().identifier().toString());
            }
            if (playerSnapshots.size() < MAX_PLAYER_SNAPSHOTS) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (playerSnapshots.size() >= MAX_PLAYER_SNAPSHOTS) {
                        break;
                    }
                    BlockPos pos = player.blockPosition();
                    playerSnapshots.add(player.level().dimension().identifier()
                            + " x=" + pos.getX() + " y=" + pos.getY() + " z=" + pos.getZ());
                }
            }
        }

        private WorldgenObservationResult toResult(MinecraftServer server, WorldgenObservationResult.EndReason endReason, Instant endedAt) {
            int loadedChunksEnd = loadedChunkCount(server);
            long memoryEndMiB = RuntimeSnapshots.memory().usedMiB();
            List<String> warnings = warnings(loadedChunksEnd, memoryEndMiB);
            return new WorldgenObservationResult(
                    requestedSeconds,
                    Math.max(0.0D, Duration.between(startedAt, endedAt).toMillis() / 1000.0D),
                    sampleCount,
                    Set.copyOf(dimensions),
                    List.copyOf(playerSnapshots),
                    loadedChunksStart,
                    loadedChunksEnd,
                    loadedChunksMin,
                    Math.max(loadedChunksMax, loadedChunksEnd),
                    sampleCount == 0 ? 0.0D : tickTotalMs / sampleCount,
                    maxTickMs,
                    memoryStartMiB,
                    memoryEndMiB,
                    warnings,
                    startedAt,
                    endedAt,
                    endReason
            );
        }

        private List<String> warnings(int loadedChunksEnd, long memoryEndMiB) {
            List<String> warnings = new ArrayList<>();
            int chunkGrowth = loadedChunksEnd - loadedChunksStart;
            if (chunkGrowth >= 250) {
                warnings.add("Loaded chunk count increased by " + chunkGrowth + " during observation.");
            }
            long memoryGrowth = memoryEndMiB - memoryStartMiB;
            if (memoryGrowth >= 512) {
                warnings.add("Used memory increased by " + memoryGrowth + " MiB during observation.");
            }
            if (maxTickMs >= 100.0D) {
                warnings.add("Sampled server tick time reached " + format(maxTickMs) + " ms.");
            }
            return warnings;
        }
    }

    private static int loadedChunkCount(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            count += level.getChunkSource().getLoadedChunksCount();
        }
        return count;
    }
}

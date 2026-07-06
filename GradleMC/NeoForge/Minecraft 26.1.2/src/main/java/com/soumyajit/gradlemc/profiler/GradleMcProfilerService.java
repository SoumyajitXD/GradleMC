package com.soumyajit.gradlemc.profiler;

import com.mojang.brigadier.Command;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.profiler.report.ProfilingReportWriter;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class GradleMcProfilerService {
    private static ProfilerSession currentSession;
    private static ProfilingReportWriter.Result latestReport;

    private GradleMcProfilerService() {
    }

    public static int start(CommandSourceStack source, ProfilerSessionConfig config) {
        if (currentSession != null && currentSession.state() == ProfilerSessionState.RUNNING) {
            source.sendFailure(Component.literal("A GradleMC profiler session is already running. Use /gradlemc profiler stop or /gradlemc profiler cancel."));
            return 0;
        }
        ProfilerSessionConfig safeConfig = config.sanitized();
        if ("*".equals(safeConfig.threadPattern()) && safeConfig.intervalMillis() <= 10) {
            source.sendSuccess(() -> Component.literal("Advanced profiler warning: all-thread sampling at " + safeConfig.intervalMillis()
                    + "ms can add overhead on large modpacks."), false);
        }
        currentSession = new ProfilerSession(safeConfig, Instant.now());
        source.sendSuccess(() -> Component.literal("GradleMC profiler started: mode=" + safeConfig.mode().id()
                + ", timeout=" + safeConfig.timeoutSeconds() + "s, interval=" + safeConfig.intervalMillis()
                + "ms, thread=" + safeConfig.threadPattern() + "."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int stop(CommandSourceStack source) {
        ProfilerSession session = currentSession;
        if (session == null || session.state() != ProfilerSessionState.RUNNING) {
            source.sendFailure(Component.literal("No GradleMC profiler session is currently running."));
            return 0;
        }
        finish(source.getServer(), ProfilerSessionState.STOPPED, source);
        return Command.SINGLE_SUCCESS;
    }

    public static int cancel(CommandSourceStack source) {
        ProfilerSession session = currentSession;
        if (session == null || session.state() != ProfilerSessionState.RUNNING) {
            source.sendFailure(Component.literal("No GradleMC profiler session is currently running."));
            return 0;
        }
        session.cancel();
        currentSession = null;
        source.sendSuccess(() -> Component.literal("GradleMC profiler cancelled. No profile was written."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int status(CommandSourceStack source) {
        ProfilerSession session = currentSession;
        if (session == null || session.state() != ProfilerSessionState.RUNNING) {
            source.sendSuccess(() -> Component.literal("GradleMC profiler: idle."), false);
            latest(source);
            return Command.SINGLE_SUCCESS;
        }
        long elapsed = Duration.between(session.startedAt(), Instant.now()).toSeconds();
        long remaining = Math.max(0L, session.config().timeoutSeconds() - elapsed);
        source.sendSuccess(() -> Component.literal("GradleMC profiler: running mode=" + session.config().mode().id()
                + ", elapsed=" + elapsed + "s, remaining=" + remaining + "s, ticks="
                + session.tickRecorder().totalRecorded() + ", slow ticks=" + session.tickRecorder().summary().slowTickCount()
                + ", CPU-lite samples=" + session.stackAggregator().sampleCount() + "."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int latest(CommandSourceStack source) {
        Path latest = latestProfileText();
        if (latest == null) {
            source.sendSuccess(() -> Component.literal("No GradleMC profiles found under " + GradleMcPaths.displayPath(GradleMcPaths.profileDirectory()) + "."), false);
            return 0;
        }
        source.sendSuccess(() -> GradleMcPaths.pathComponent("Latest GradleMC profile: ", latest), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int summary(CommandSourceStack source) {
        Path latest = latestProfileText();
        if (latest == null) {
            source.sendFailure(Component.literal("No GradleMC profile summary is available yet."));
            return 0;
        }
        try (Stream<String> lines = Files.lines(latest)) {
            lines.filter(line -> line.startsWith("Summary:")
                            || line.startsWith("Mode:")
                            || line.startsWith("Average MSPT:")
                            || line.startsWith("Max MSPT:")
                            || line.startsWith("Slow ticks:")
                            || line.startsWith("Samples:"))
                    .limit(8)
                    .forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Could not read latest GradleMC profile: " + safeMessage(exception)));
            return 0;
        }
    }

    public static int open(CommandSourceStack source) {
        source.sendSuccess(() -> GradleMcPaths.pathComponent("GradleMC profiles folder: ", GradleMcPaths.profileDirectory()), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int export(CommandSourceStack source) {
        Path latest = latestProfileText();
        if (latest == null) {
            source.sendFailure(Component.literal("No GradleMC profile is available to export."));
            return 0;
        }
        source.sendSuccess(() -> GradleMcPaths.pathComponent("Latest profile is already local/offline: ", latest), false);
        return Command.SINGLE_SUCCESS;
    }

    public static boolean isRunning() {
        return currentSession != null && currentSession.state() == ProfilerSessionState.RUNNING;
    }

    public static String overlayStatusLine() {
        ProfilerSession session = currentSession;
        if (session == null || session.state() != ProfilerSessionState.RUNNING) {
            return "Profiler: idle";
        }
        long elapsed = Duration.between(session.startedAt(), Instant.now()).toSeconds();
        long remaining = Math.max(0L, session.config().timeoutSeconds() - elapsed);
        return "Profiler: " + session.config().mode().id() + " " + elapsed + "s/" + remaining + "s, samples "
                + session.stackAggregator().sampleCount() + ", slow " + session.tickRecorder().summary().slowTickCount();
    }

    public static void onServerTickStart(ServerTickEvent.Pre event) {
        ProfilerSession session = currentSession;
        if (session == null || session.state() != ProfilerSessionState.RUNNING) {
            return;
        }
        session.onTickStart();
    }

    public static void onServerTickEnd(ServerTickEvent.Post event) {
        ProfilerSession session = currentSession;
        if (session == null || session.state() != ProfilerSessionState.RUNNING) {
            return;
        }
        if (session.onTickEnd(event.getServer())) {
            finish(event.getServer(), ProfilerSessionState.COMPLETED, null);
        }
    }

    private static void finish(MinecraftServer server, ProfilerSessionState state, CommandSourceStack source) {
        ProfilerSession session = currentSession;
        if (session == null) {
            return;
        }
        currentSession = null;
        try {
            latestReport = session.finish(state);
            Component message = GradleMcPaths.pathComponent("GradleMC profile complete. Summary: ", latestReport.textPath());
            if (source != null) {
                source.sendSuccess(() -> message, false);
            } else {
                GradleMC.LOGGER.info(message.getString());
            }
        } catch (IOException | RuntimeException exception) {
            GradleMC.LOGGER.warn("GradleMC profiler report failed", exception);
            if (source != null) {
                source.sendFailure(Component.literal("GradleMC profiler failed to write report: " + safeMessage(exception)));
            }
        }
    }

    private static Path latestProfileText() {
        if (latestReport != null && Files.isRegularFile(latestReport.textPath())) {
            return latestReport.textPath();
        }
        if (!Files.isDirectory(GradleMcPaths.profileDirectory())) {
            return null;
        }
        try (Stream<Path> paths = Files.list(GradleMcPaths.profileDirectory())) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("gradlemc-profile-"))
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .max(Comparator.comparing(GradleMcProfilerService::modifiedTimeSafe))
                    .orElse(null);
        } catch (IOException exception) {
            return null;
        }
    }

    private static long modifiedTimeSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    public static ProfilerSessionConfig parseOptions(String raw) {
        ProfilerSessionConfig defaults = ProfilerSessionConfig.defaults();
        int timeout = defaults.timeoutSeconds();
        int interval = defaults.intervalMillis();
        String thread = defaults.threadPattern();
        double slowTicks = defaults.onlyTicksOverMillis();
        boolean includeSleeping = defaults.includeSleeping();
        ProfilerMode mode = defaults.mode();
        List<String> tokens = raw == null || raw.isBlank() ? List.of() : List.of(raw.trim().split("\\s+"));
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).toLowerCase(Locale.ROOT);
            String value = i + 1 < tokens.size() ? tokens.get(++i) : "";
            switch (token) {
                case "--timeout" -> timeout = parseInt(value, timeout);
                case "--interval" -> interval = parseInt(value, interval);
                case "--thread" -> thread = value;
                case "--only-ticks-over" -> slowTicks = parseDouble(value, slowTicks);
                case "--include-sleeping" -> includeSleeping = Boolean.parseBoolean(value);
                case "--mode" -> mode = ProfilerMode.parse(value);
                default -> i--;
            }
        }
        return new ProfilerSessionConfig(timeout, interval, thread, slowTicks, includeSleeping, mode).sanitized();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}

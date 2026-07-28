package com.soumyajit.gradlemc.metrics;

import com.soumyajit.gradlemc.GradleMC;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/** Concise console-safe presentation of the canonical telemetry and monitor state. */
public final class HealthCommandService {
    private HealthCommandService() { }
    public static int health(CommandSourceStack source) {
        var snapshot = MeasurementHub.instance().serverPerformanceSnapshot();
        if (snapshot.availability() != ServerPerformanceSnapshot.Availability.AVAILABLE) {
            source.sendFailure(Component.literal("Server tick evidence is unavailable or warming up."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Server timing: " + snapshot.sampleCount() + " samples, average "
                + String.format(java.util.Locale.ROOT, "%.2f", snapshot.averageTps()) + " TPS, "
                + String.format(java.util.Locale.ROOT, "%.2f", snapshot.averageMspt()) + " MSPT, max "
                + String.format(java.util.Locale.ROOT, "%.2f", snapshot.maximumMspt()) + " MSPT (" + snapshot.freshness() + ")."), false);
        return 1;
    }
    public static int start(CommandSourceStack source) {
        if (!TickMonitorService.start(TickMonitorService.DEFAULT_THRESHOLD_MILLIS, 0.0D)) { source.sendFailure(Component.translatable("command.gradlemc.tickmonitor.running")); return 0; }
        source.sendSuccess(() -> Component.translatable("command.gradlemc.tickmonitor.started"), false); return 1;
    }
    public static int stop(CommandSourceStack source) {
        if (!TickMonitorService.stop()) { source.sendFailure(Component.translatable("command.gradlemc.tickmonitor.idle")); return 0; }
        source.sendSuccess(() -> Component.translatable("command.gradlemc.tickmonitor.stopped"), false); return 1;
    }
    public static int status(CommandSourceStack source) {
        var state = TickMonitorService.snapshot();
        source.sendSuccess(() -> Component.translatable("command.gradlemc.tickmonitor.status", state.running() ? "running" : "idle", String.format(java.util.Locale.ROOT, "%.2f", state.thresholdMillis()), state.observedTicks(), state.incidents()), false);
        return 1;
    }
}

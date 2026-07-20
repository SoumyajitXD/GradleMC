package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.client.overlay.FpsRollingStatsCalculator;
import com.soumyajit.gradlemc.metrics.FpsTestResult;
import com.soumyajit.gradlemc.task.FabricDiagnosticService;
import com.soumyajit.gradlemc.task.TaskEnvironment;
import net.minecraft.client.Minecraft;

import java.util.EnumSet;
import java.util.HashMap;

/** Client-only entry for the FPS workflow. It consumes the existing authoritative sampler; it never creates a second sampler. */
public final class ClientWorkflowBridge {
    private ClientWorkflowBridge() { }

    public static StartResult startFpsWorkflow() {
        if (FpsTestManager.isRunning()) return new StartResult(false, "An FPS test is already running; finish or stop it before starting the FPS workflow.");
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return new StartResult(false, "FPS workflow requires an active client world and player.");
        FpsTestResult completed = FpsTestManager.latestResult().orElse(null);
        FpsRollingStatsCalculator.Snapshot rolling = FpsTestManager.latestSnapshot(false);
        Double fps = completed != null && completed.hasSamples() ? completed.averageFps() : rolling.averageFps();
        if (fps == null || !Double.isFinite(fps) || fps <= 0.0D) return new StartResult(false, "No authoritative rendered-frame FPS sample is available yet. Run an FPS test first.");
        HashMap<String, String> snapshot = new HashMap<>(FabricDiagnosticService.captureSnapshot());
        snapshot.put("fps.average", String.format(java.util.Locale.ROOT, "%.3f", fps));
        if (completed != null && completed.hasSamples()) {
            snapshot.put("fps.samples", Integer.toString(completed.samples()));
            snapshot.put("fps.duration_seconds", String.format(java.util.Locale.ROOT, "%.3f", completed.actualSeconds()));
        }
        try {
            FabricDiagnosticService.start("fps", snapshot, EnumSet.of(TaskEnvironment.CLIENT, TaskEnvironment.WORLD, TaskEnvironment.PLAYER));
            return new StartResult(true, "FPS workflow started from the authoritative rendered-frame snapshot.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return new StartResult(false, exception.getMessage() == null ? "The FPS workflow could not start." : exception.getMessage());
        }
    }

    public record StartResult(boolean started, String message) { }
}

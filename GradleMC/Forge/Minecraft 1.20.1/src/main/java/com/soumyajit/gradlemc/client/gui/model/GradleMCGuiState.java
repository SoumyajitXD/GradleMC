package com.soumyajit.gradlemc.client.gui.model;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.client.FpsTestManager;
import com.soumyajit.gradlemc.client.overlay.GradleMCStatsOverlay;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import com.soumyajit.gradlemc.network.GuiStatusSnapshot;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.versions.forge.ForgeVersion;

import java.nio.file.Path;

public record GradleMCGuiState(
        String modVersion,
        String minecraftVersion,
        String forgeVersion,
        String playerName,
        SmartAIStatus smartAIStatus,
        GuiStatusSnapshot guiStatus,
        long smartAIStatusAgeMillis,
        RuntimeSnapshots.MemorySnapshot memory,
        int currentFps,
        double rollingAverageFps,
        Double rollingOnePercentLowFps,
        Double rollingPointOnePercentLowFps,
        boolean reportsEnabled,
        boolean ruleChecksEnabled,
        boolean smartDiagnosticsEnabled,
        boolean adaptiveBaselineEnabled,
        boolean debugSmartAILogging,
        boolean allowHighIntensityEvents,
        boolean reduceIntensityAfterDeath,
        boolean fpsTestRunning,
        boolean performanceTestRunning,
        boolean worldgenObservationRunning,
        DiagnosticTestProgress fpsProgress,
        DiagnosticTestProgress performanceProgress,
        DiagnosticTestProgress worldgenProgress,
        String latestFpsReportPath,
        int maxThreatLevel,
        int eventCooldownTicks,
        int ambienceCooldownTicks,
        double adaptiveDifficultyMultiplier
) {
    private static final String MOD_VERSION = ModList.get().getModContainerById(GradleMC.MOD_ID)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("");
    private static final String MINECRAFT_VERSION = SharedConstants.getCurrentVersion().getName();
    private static final String FORGE_VERSION = ForgeVersion.getVersion();

    public static GradleMCGuiState capture(SmartAIStatus status) {
        Minecraft minecraft = Minecraft.getInstance();
        String player = minecraft.player == null ? "" : minecraft.player.getGameProfile().getName();
        SmartAIStatus safeStatus = status == null ? SmartAIStatus.disabled() : status;
        GuiStatusSnapshot guiStatus = GradleMCGuiBridge.latestGuiStatus();
        DiagnosticTestProgress fpsProgress = FpsTestManager.progress();
        DiagnosticTestProgress performanceProgress = guiStatus.performanceProgress();
        DiagnosticTestProgress worldgenProgress = guiStatus.worldgenProgress();
        var rollingFps = GradleMCStatsOverlay.latestFpsSnapshot();
        return new GradleMCGuiState(
                MOD_VERSION,
                MINECRAFT_VERSION,
                FORGE_VERSION,
                player,
                safeStatus,
                guiStatus,
                GradleMCGuiBridge.smartAIStatusAgeMillis(),
                RuntimeSnapshots.memory(),
                (int) Math.round(rollingFps.currentFps()),
                rollingFps.averageFps(),
                rollingFps.onePercentLowFps(),
                rollingFps.pointOnePercentLowFps(),
                GradleMCConfig.REPORTS_ENABLED.get(),
                GradleMCConfig.ENABLE_RULE_CHECKS.get(),
                GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get(),
                GradleMCConfig.ADAPTIVE_BASELINE_ENABLED.get(),
                GradleMCConfig.DEBUG_SMART_AI_LOGGING.get(),
                GradleMCConfig.ALLOW_HIGH_INTENSITY_EVENTS.get(),
                GradleMCConfig.REDUCE_INTENSITY_AFTER_DEATH.get(),
                fpsProgress.running(),
                performanceProgress.running(),
                worldgenProgress.running(),
                fpsProgress,
                performanceProgress,
                worldgenProgress,
                display(FpsTestManager.latestReportPath()),
                GradleMCConfig.MAX_THREAT_LEVEL.get(),
                GradleMCConfig.EVENT_COOLDOWN_TICKS.get(),
                GradleMCConfig.AMBIENCE_COOLDOWN_TICKS.get(),
                GradleMCConfig.ADAPTIVE_DIFFICULTY_MULTIPLIER.get()
        );
    }

    private static String display(Path path) {
        return path == null ? "" : GradleMcPaths.displayPath(path);
    }
}

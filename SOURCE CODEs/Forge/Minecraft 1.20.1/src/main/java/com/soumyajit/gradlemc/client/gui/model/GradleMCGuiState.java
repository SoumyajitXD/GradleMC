package com.soumyajit.gradlemc.client.gui.model;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.versions.forge.ForgeVersion;

public record GradleMCGuiState(
        String modVersion,
        String minecraftVersion,
        String forgeVersion,
        String playerName,
        SmartAIStatus smartAIStatus,
        long smartAIStatusAgeMillis,
        boolean reportsEnabled,
        boolean ruleChecksEnabled,
        boolean smartDiagnosticsEnabled,
        boolean adaptiveBaselineEnabled,
        boolean debugSmartAILogging,
        boolean allowHighIntensityEvents,
        boolean reduceIntensityAfterDeath,
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
        return new GradleMCGuiState(
                MOD_VERSION,
                MINECRAFT_VERSION,
                FORGE_VERSION,
                player,
                safeStatus,
                GradleMCGuiBridge.smartAIStatusAgeMillis(),
                GradleMCConfig.REPORTS_ENABLED.get(),
                GradleMCConfig.ENABLE_RULE_CHECKS.get(),
                GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get(),
                GradleMCConfig.ADAPTIVE_BASELINE_ENABLED.get(),
                GradleMCConfig.DEBUG_SMART_AI_LOGGING.get(),
                GradleMCConfig.ALLOW_HIGH_INTENSITY_EVENTS.get(),
                GradleMCConfig.REDUCE_INTENSITY_AFTER_DEATH.get(),
                GradleMCConfig.MAX_THREAT_LEVEL.get(),
                GradleMCConfig.EVENT_COOLDOWN_TICKS.get(),
                GradleMCConfig.AMBIENCE_COOLDOWN_TICKS.get(),
                GradleMCConfig.ADAPTIVE_DIFFICULTY_MULTIPLIER.get()
        );
    }
}

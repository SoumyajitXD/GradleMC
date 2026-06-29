# Common-Core Extraction Audit

This audit is a planning artifact. It does not claim Fabric, NeoForge, or new Minecraft-version support.

## Extraction Rules

- Scoring, profiler math, report math, FPS low calculations, issue bundle models, and serialization helpers belong in `common-core`.
- Command registration belongs in loader adapters.
- GUI, keybind, HUD overlay, FPS polling, and screens belong in client adapters.
- Game directory and output paths should sit behind a platform service.
- Mod list lookup should sit behind a platform service.
- Server tick sampling should sit behind a platform service.
- Client FPS sampling should sit behind a client platform service.

## Pure Java Common-Core Ready

These files are already mostly independent of Minecraft/Forge classes and are strong first extraction candidates:

- `src/main/java/com/soumyajit/gradlemc/ai/AdaptiveRiskCalculator.java`
- `src/main/java/com/soumyajit/gradlemc/ai/ThreatLevel.java`
- `src/main/java/com/soumyajit/gradlemc/ai/SmartAIStatus.java`
- `src/main/java/com/soumyajit/gradlemc/check/CheckCategory.java`
- `src/main/java/com/soumyajit/gradlemc/check/CheckResult.java`
- `src/main/java/com/soumyajit/gradlemc/check/Severity.java`
- `src/main/java/com/soumyajit/gradlemc/client/overlay/FpsRollingStatsCalculator.java`
- `src/main/java/com/soumyajit/gradlemc/config/OverlayDefaults.java`
- `src/main/java/com/soumyajit/gradlemc/metrics/DiagnosticTestProgress.java`
- `src/main/java/com/soumyajit/gradlemc/metrics/FpsTestResult.java`
- `src/main/java/com/soumyajit/gradlemc/metrics/PerformanceTestResult.java`
- `src/main/java/com/soumyajit/gradlemc/metrics/WorldgenObservationResult.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/ProfilerMode.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/ProfilerSessionConfig.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/ProfilerSessionState.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/memory/GcEventTracker.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/memory/MemoryPressureTracker.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/sampling/StackTraceAggregator.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/sampling/ThreadSampler.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/tick/SlowTickDetector.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/tick/SlowTickSnapshot.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/tick/TickRecord.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/tick/TickSummary.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/tick/TickTimelineRecorder.java`
- `src/main/java/com/soumyajit/gradlemc/report/Report.java`
- `src/main/java/com/soumyajit/gradlemc/report/ReportFileNames.java`
- `src/main/java/com/soumyajit/gradlemc/rules/RiskRule.java`
- `src/main/java/com/soumyajit/gradlemc/rules/RiskRuleSet.java`
- `src/main/java/com/soumyajit/gradlemc/rules/RiskRuleType.java`
- `src/main/java/com/soumyajit/gradlemc/smart/AdaptiveBaseline.java`
- `src/main/java/com/soumyajit/gradlemc/smart/AdaptiveThresholds.java`
- `src/main/java/com/soumyajit/gradlemc/smart/AnomalySeverity.java`
- `src/main/java/com/soumyajit/gradlemc/smart/ConfidenceLevel.java`
- `src/main/java/com/soumyajit/gradlemc/smart/DiagnosticEvidence.java`
- `src/main/java/com/soumyajit/gradlemc/smart/DiagnosticFinding.java`
- `src/main/java/com/soumyajit/gradlemc/smart/RiskLevel.java`
- `src/main/java/com/soumyajit/gradlemc/smart/SmartMetricSnapshots.java`
- `src/main/java/com/soumyajit/gradlemc/smart/SmartRecommendation.java`
- `src/main/java/com/soumyajit/gradlemc/smart/StabilityScore.java`
- `src/main/java/com/soumyajit/gradlemc/util/RuntimeSnapshots.java`

Recommended move: create `common-core` and move these first, then wire the Forge project to depend on it.

## Minecraft-Common Bridge

These files contain useful cross-loader concepts but currently import Minecraft or Forge APIs:

- `src/main/java/com/soumyajit/gradlemc/check/CheckContext.java`
- `src/main/java/com/soumyajit/gradlemc/check/StabilityCheck.java`
- `src/main/java/com/soumyajit/gradlemc/check/BasicCheckRegistry.java`
- `src/main/java/com/soumyajit/gradlemc/check/impl/ConfigSanityCheck.java`
- `src/main/java/com/soumyajit/gradlemc/check/impl/JavaVersionCheck.java`
- `src/main/java/com/soumyajit/gradlemc/check/impl/LoadedModsCheck.java`
- `src/main/java/com/soumyajit/gradlemc/check/impl/LogNoiseClassificationCheck.java`
- `src/main/java/com/soumyajit/gradlemc/check/impl/MemoryInfoCheck.java`
- `src/main/java/com/soumyajit/gradlemc/check/impl/PerformancePlaceholderCheck.java`
- `src/main/java/com/soumyajit/gradlemc/check/impl/ReportDirectoryCheck.java`
- `src/main/java/com/soumyajit/gradlemc/check/impl/RiskRuleCheck.java`
- `src/main/java/com/soumyajit/gradlemc/metrics/PerformanceTestManager.java`
- `src/main/java/com/soumyajit/gradlemc/metrics/WorldgenObservationManager.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/GradleMcProfilerService.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/ProfilerSession.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/report/ModAttribution.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/report/ProfilingReportWriter.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/report/ProfilingSummary.java`
- `src/main/java/com/soumyajit/gradlemc/profiler/report/ProfilingSummaryBuilder.java`
- `src/main/java/com/soumyajit/gradlemc/report/FpsTestReportWriter.java`
- `src/main/java/com/soumyajit/gradlemc/report/IssueBundleExporter.java`
- `src/main/java/com/soumyajit/gradlemc/report/PerformanceTestReportWriter.java`
- `src/main/java/com/soumyajit/gradlemc/report/ReportEnvironment.java`
- `src/main/java/com/soumyajit/gradlemc/report/ReportWriter.java`
- `src/main/java/com/soumyajit/gradlemc/report/WorldgenObservationReportWriter.java`
- `src/main/java/com/soumyajit/gradlemc/rules/RiskRuleLoader.java`
- `src/main/java/com/soumyajit/gradlemc/smart/AdaptiveBaselineStore.java`
- `src/main/java/com/soumyajit/gradlemc/smart/StabilityAdvisor.java`
- `src/main/java/com/soumyajit/gradlemc/util/GradleMcPaths.java`

Recommended move: define interfaces such as `PlatformPaths`, `LoadedModsView`, `CommandFeedback`, `ServerTickSampler`, `ClientFpsSampler`, and `ReportOutputService`. Keep Minecraft types out of the pure common layer.

## Forge Adapter

These are Forge-specific or current Forge entrypoint/integration code:

- `src/main/java/com/soumyajit/gradlemc/GradleMC.java`
- `src/main/java/com/soumyajit/gradlemc/command/GradleMcCommands.java`
- `src/main/java/com/soumyajit/gradlemc/command/FpsTestCommandBridge.java`
- `src/main/java/com/soumyajit/gradlemc/config/GradleMCConfig.java`
- `src/main/java/com/soumyajit/gradlemc/network/GradleMCGuiBridge.java`
- `src/main/java/com/soumyajit/gradlemc/network/GradleMCNetwork.java`
- `src/main/java/com/soumyajit/gradlemc/network/GuiStatusSnapshot.java`
- `src/main/java/com/soumyajit/gradlemc/network/OpenGradleMCGuiPacket.java`
- `src/main/java/com/soumyajit/gradlemc/network/RequestSmartAIStatusPacket.java`
- `src/main/java/com/soumyajit/gradlemc/network/SyncSmartAIStatusPacket.java`

Recommended move: split command tree construction from Forge event registration. Keep packet channel creation and Forge config in the Forge adapter.

## Client-Only GUI And Overlay

These files must stay isolated from dedicated server classloading:

- `src/main/java/com/soumyajit/gradlemc/client/ClientEventHandler.java`
- `src/main/java/com/soumyajit/gradlemc/client/ClientModEventHandler.java`
- `src/main/java/com/soumyajit/gradlemc/client/FpsTestManager.java`
- `src/main/java/com/soumyajit/gradlemc/client/gui/GradleMCGuiOpener.java`
- `src/main/java/com/soumyajit/gradlemc/client/gui/GradleMCGuiSection.java`
- `src/main/java/com/soumyajit/gradlemc/client/gui/GradleMCScreen.java`
- `src/main/java/com/soumyajit/gradlemc/client/gui/model/GradleMCGuiState.java`
- `src/main/java/com/soumyajit/gradlemc/client/input/GradleMCKeyMappings.java`
- `src/main/java/com/soumyajit/gradlemc/client/overlay/ClientSystemStats.java`
- `src/main/java/com/soumyajit/gradlemc/client/overlay/GradleMCStatsOverlay.java`
- `src/main/java/com/soumyajit/gradlemc/client/overlay/OverlayConfigActions.java`
- `src/main/java/com/soumyajit/gradlemc/client/overlay/OverlayPosition.java`

Recommended move: create per-loader client adapters and keep common GUI state models free of direct `Minecraft.getInstance()` and Forge APIs where possible.

## Version-Sensitive Code

These areas are likely to change across 1.20.x, 1.21.x, and 26.x:

- Brigadier command registration and `CommandSourceStack` usage in `GradleMcCommands`.
- Forge event types in tick, command, config, networking, and client handlers.
- Networking packet registration in `GradleMCNetwork`.
- GUI rendering APIs in `GradleMCScreen` and overlay rendering APIs in `GradleMCStatsOverlay`.
- Chunk, block entity, entity scan APIs in `GradleMcCommands`.
- Server tick lifecycle and player/world access in `PerformanceTestManager`, `WorldgenObservationManager`, and `ProfilerSession`.

Recommended move: put version-sensitive calls behind minimal shims before adding more variants.

## Risky Duplicated Logic

These features are useful but should not be reimplemented per loader:

- Smart Diagnostics scoring in `StabilityAdvisor`.
- Adaptive risk scoring in `AdaptiveRiskCalculator`.
- FPS low calculations in `FpsRollingStatsCalculator`.
- Profiler aggregation and report summary builders.
- Report filename and text/JSON output formatting.
- Risk rule parsing and checks.

Recommended move: extract once, then call through platform services.

## Not Worth Porting Yet

These should wait until the first adapter is proven:

- Full GUI parity across every loader/version.
- 26.x support claims.
- Publishing workflow per variant.
- Runtime smoke automation for disabled variants.
- Any target that requires large copy-pasted command or GUI implementations.

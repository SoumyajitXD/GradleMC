package com.soumyajit.gradlemc;

import com.mojang.logging.LogUtils;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.command.GradleMcCommands;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.foundation.FoundationService;
import com.soumyajit.gradlemc.foundation.GradleMcRuntimeExecutor;
import com.soumyajit.gradlemc.foundation.ForgeGameThreadBridgeService;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.metrics.ServerHealthTelemetry;
import com.soumyajit.gradlemc.metrics.TickMonitorService;
import com.soumyajit.gradlemc.metrics.MeasurementHub;
import com.soumyajit.gradlemc.metrics.ServerPerformanceSnapshot;
import com.soumyajit.gradlemc.performance.PerformanceService;
import com.soumyajit.gradlemc.investigation.session.InvestigationCommandService;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import com.soumyajit.gradlemc.profiler.GradleMcProfilerService;
import com.soumyajit.gradlemc.startup.ResourceReloadTimingService;
import com.soumyajit.gradlemc.startup.StartupTimingService;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

@Mod(GradleMC.MOD_ID)
public class GradleMC {
    public static final String PRODUCT_NAME = "GradleMC";
    public static final String MOD_ID = "gradlemc";
    public static final String CURRENT_VERSION = "1.0.4";
    public static final String CURRENT_LOADER_NAME = "Forge";
    public static final String CURRENT_MINECRAFT_VERSION = "1.20.1";
    public static final String CURRENT_VARIANT_ID = "forge-1.20.1";
    public static final String CURRENT_DISPLAY_VARIANT = "Forge 1.20.1";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static int guardSampleTicks;

    public GradleMC(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, GradleMCConfig.SPEC);
        GradleMCNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(GradleMcCommands::register);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(PerformanceTestManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(WorldgenObservationManager::onServerTick);
        // Register the shared START/END timing producer before every timing consumer.
        MinecraftForge.EVENT_BUS.addListener(GradleMC::onServerTickHealth);
        MinecraftForge.EVENT_BUS.addListener(GradleMcProfilerService::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(StartupTimingService::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(StartupTimingService::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(StartupTimingService::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(ResourceReloadTimingService::onReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(ResourceReloadTimingService::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(ForgeGameThreadBridgeService::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(ForgeGameThreadBridgeService::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(GradleMC::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(GradleMC::onServerStarted);
        StartupTimingService.initializationCompleted();
        LOGGER.info("GradleMC command scaffold loaded");
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        // Complete queued game-thread captures first so their owning Investigation can persist a
        // terminal cancellation rather than waiting for its ordinary request timeout.
        ForgeGameThreadBridgeService.onServerStopped(event);
        InvestigationCommandService.onServerStopped();
        GradleMcProfilerService.shutdown();
        FoundationService.resetExecutor();
        GradleMcRuntimeExecutor.shutdown();
        MeasurementHub.instance().resetServerPerformance(event.getServer().isDedicatedServer());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        GradleMcRuntimeExecutor.start();
        InvestigationCommandService.onServerStarted();
        GradleMcProfilerService.onServerStarted();
        MeasurementHub.instance().resetServerPerformance(event.getServer().isDedicatedServer());
    }

    public static ServerHealthTelemetry serverHealth() { return new ServerHealthTelemetry(); }

    private static void onServerTickHealth(TickEvent.ServerTickEvent event) {
        long now = System.nanoTime();
        if (event.phase == TickEvent.Phase.START) {
            MeasurementHub.instance().beginServerTick(now);
            return;
        }
        if (event.phase == TickEvent.Phase.END) {
            long started = System.nanoTime();
            MeasurementHub.instance().completeServerTick(now);
            ServerPerformanceSnapshot snapshot = MeasurementHub.instance().serverPerformanceSnapshot();
            if (Double.isFinite(snapshot.currentMspt())) TickMonitorService.onTick((long) (snapshot.currentMspt() * 1_000_000D));
            if (++guardSampleTicks >= 20) {
                guardSampleTicks = 0;
                var memory = MeasurementHub.instance().memorySnapshot(false);
                double heapPressure = Double.isFinite(memory.usedPercent()) ? memory.usedPercent() / 100D : Double.NaN;
                var overhead = PerformanceService.overhead().snapshot();
                PerformanceService.guard().observe(now, new com.soumyajit.gradlemc.performance.PerformanceGuard.Evidence(
                        Double.NaN, snapshot.averageMspt(), heapPressure, overhead.queueDepth(), 0, Double.NaN,
                        false, snapshot.freshness() == ServerPerformanceSnapshot.Freshness.FRESH,
                        memory.freshness() == com.soumyajit.gradlemc.metrics.MemorySnapshot.Freshness.FRESH,
                        true, false, false));
            }
            PerformanceService.overhead().record(com.soumyajit.gradlemc.performance.GradleMcOverheadMonitor.Category.SERVER_TICK_INGESTION, System.nanoTime() - started);
        }
    }
}

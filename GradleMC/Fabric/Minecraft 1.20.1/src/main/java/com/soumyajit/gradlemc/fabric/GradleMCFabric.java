package com.soumyajit.gradlemc.fabric;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.command.GradleMcCommands;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import com.soumyajit.gradlemc.profiler.GradleMcProfilerService;
import com.soumyajit.gradlemc.task.FabricDiagnosticService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GradleMCFabric implements ModInitializer {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    @Override
    public void onInitialize() {
        if (!INITIALIZED.compareAndSet(false, true)) return;
        GradleMCConfig.SPEC.load();
        GradleMCNetwork.register();
        CommandRegistrationCallback.EVENT.register(GradleMcCommands::register);
        ServerTickEvents.END_SERVER_TICK.register(AdaptiveSmartAIManager::onEndServerTick);
        ServerTickEvents.END_SERVER_TICK.register(PerformanceTestManager::onEndServerTick);
        ServerTickEvents.END_SERVER_TICK.register(WorldgenObservationManager::onEndServerTick);
        ServerTickEvents.START_SERVER_TICK.register(GradleMcProfilerService::onStartServerTick);
        ServerTickEvents.END_SERVER_TICK.register(GradleMcProfilerService::onEndServerTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(AdaptiveSmartAIManager::onAllowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(AdaptiveSmartAIManager::onAfterDeath);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                {
                    AdaptiveSmartAIManager.onPlayerLeave(handler.getPlayer());
                    GradleMCNetwork.clearPlayer(handler.getPlayer());
                });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            GradleMcProfilerService.onServerStopping();
            FabricDiagnosticService.onServerStopping(server.isDedicatedServer());
        });
        GradleMC.LOGGER.info("{} {} initialized", GradleMC.PRODUCT_NAME, GradleMC.CURRENT_DISPLAY_VARIANT);
    }
}

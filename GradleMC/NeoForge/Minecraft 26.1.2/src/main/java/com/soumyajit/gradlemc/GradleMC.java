package com.soumyajit.gradlemc;

import com.mojang.logging.LogUtils;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.command.GradleMcCommands;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import com.soumyajit.gradlemc.profiler.GradleMcProfilerService;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(GradleMC.MOD_ID)
public class GradleMC {
    public static final String PRODUCT_NAME = "GradleMC";
    public static final String MOD_ID = "gradlemc";
    public static final String CURRENT_LOADER_NAME = "NeoForge";
    public static final String CURRENT_MINECRAFT_VERSION = "26.1.2";
    public static final String CURRENT_VARIANT_ID = "neoforge-26.1.2";
    public static final String CURRENT_DISPLAY_VARIANT = "NeoForge 26.1.2";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GradleMC(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, GradleMCConfig.SPEC);
        modEventBus.addListener(GradleMCNetwork::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> GradleMcCommands.register(event));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> AdaptiveSmartAIManager.onServerTick(event));
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Pre event) -> AdaptiveSmartAIManager.onLivingHurt(event));
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> AdaptiveSmartAIManager.onLivingDeath(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> AdaptiveSmartAIManager.onPlayerLoggedOut(event));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> PerformanceTestManager.onServerTick(event));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> WorldgenObservationManager.onServerTick(event));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> GradleMcProfilerService.onServerTickStart(event));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> GradleMcProfilerService.onServerTickEnd(event));
        registerClientHooks(modEventBus);
        LOGGER.info("GradleMC NeoForge 26.1.2 loaded");
    }

    private static void registerClientHooks(IEventBus modEventBus) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }
        try {
            Class.forName("com.soumyajit.gradlemc.client.ClientBootstrap")
                    .getMethod("register", IEventBus.class)
                    .invoke(null, modEventBus);
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Could not register GradleMC client hooks", exception);
        }
    }
}

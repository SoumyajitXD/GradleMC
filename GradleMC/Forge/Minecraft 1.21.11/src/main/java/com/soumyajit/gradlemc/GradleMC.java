package com.soumyajit.gradlemc;

import com.mojang.logging.LogUtils;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.command.GradleMcCommands;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import com.soumyajit.gradlemc.profiler.GradleMcProfilerService;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(GradleMC.MOD_ID)
public class GradleMC {
    public static final String PRODUCT_NAME = "GradleMC";
    public static final String MOD_ID = "gradlemc";
    public static final String CURRENT_LOADER_NAME = "Forge";
    public static final String CURRENT_MINECRAFT_VERSION = "1.21.11";
    public static final String CURRENT_VARIANT_ID = "forge-1.21.11";
    public static final String CURRENT_DISPLAY_VARIANT = "Forge 1.21.11";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GradleMC(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, GradleMCConfig.SPEC);
        GradleMCNetwork.register();
        RegisterCommandsEvent.BUS.addListener(GradleMcCommands::register);
        TickEvent.ServerTickEvent.Post.BUS.addListener(AdaptiveSmartAIManager::onServerTick);
        LivingHurtEvent.BUS.addListener(AdaptiveSmartAIManager::onLivingHurt);
        LivingDeathEvent.BUS.addListener(AdaptiveSmartAIManager::onLivingDeath);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(AdaptiveSmartAIManager::onPlayerLoggedOut);
        TickEvent.ServerTickEvent.Post.BUS.addListener(PerformanceTestManager::onServerTick);
        TickEvent.ServerTickEvent.Post.BUS.addListener(WorldgenObservationManager::onServerTick);
        TickEvent.ServerTickEvent.Pre.BUS.addListener(GradleMcProfilerService::onServerTickStart);
        TickEvent.ServerTickEvent.Post.BUS.addListener(GradleMcProfilerService::onServerTickEnd);
        registerClientHooks();
        LOGGER.info("GradleMC command scaffold loaded");
    }

    private static void registerClientHooks() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        try {
            Class.forName("com.soumyajit.gradlemc.client.ClientBootstrap")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Could not register GradleMC client hooks", exception);
        }
    }
}

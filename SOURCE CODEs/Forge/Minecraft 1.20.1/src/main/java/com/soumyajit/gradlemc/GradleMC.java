package com.soumyajit.gradlemc;

import com.mojang.logging.LogUtils;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.command.GradleMcCommands;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(GradleMC.MOD_ID)
public class GradleMC {
    public static final String MOD_ID = "gradlemc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GradleMC(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, GradleMCConfig.SPEC);
        GradleMCNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(GradleMcCommands::register);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(AdaptiveSmartAIManager::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(PerformanceTestManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(WorldgenObservationManager::onServerTick);
        LOGGER.info("GradleMC command scaffold loaded");
    }
}

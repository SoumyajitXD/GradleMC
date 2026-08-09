package com.soumyajit.gradlemc

import com.mojang.logging.LogUtils
import com.soumyajit.gradlemc.client.GradleMcClientBootstrap
import com.soumyajit.gradlemc.command.GradleMcCommands
import com.soumyajit.gradlemc.config.ForgeGradleMCConfig
import com.soumyajit.gradlemc.diagnostics.OperatingSystemInfoProvider
import com.soumyajit.gradlemc.network.GradleMcNetwork
import com.soumyajit.gradlemc.performance.PerformanceService
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig

@Mod(GradleMC.MOD_ID)
class GradleMC {
    init {
        // Forge 1.20.1/KFF 4.12.0 exposes no non-deprecated config-registration context.
        @Suppress("DEPRECATION")
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeGradleMCConfig.spec, ForgeGradleMCConfig.FILE_NAME)
        OperatingSystemInfoProvider.initialize()
        GradleMcNetwork.register()
        DistExecutor.safeRunWhenOn(Dist.CLIENT) {
            DistExecutor.SafeRunnable(GradleMcClientBootstrap::bootstrap)
        }
        // Kotlin object callable references capture and eagerly initialize their singleton receiver.
        // Non-capturing adapters keep service initialization owned by the event that first uses it.
        MinecraftForge.EVENT_BUS.addListener { event: RegisterCommandsEvent -> GradleMcCommands.register(event) }
        MinecraftForge.EVENT_BUS.addListener { event: TickEvent.ServerTickEvent -> PerformanceService.onServerTick(event) }
        MinecraftForge.EVENT_BUS.addListener { event: ServerStoppedEvent -> PerformanceService.onServerStopped(event) }
        LOGGER.info("GradleMC Next {} bootstrap loaded", VERSION)
    }

    companion object {
        const val MOD_ID = "gradlemc"
        const val VERSION = "1.1.0"
        const val MINECRAFT_VERSION = "1.20.1"
        const val FORGE_VERSION = "47.4.22"
        const val KOTLIN_FOR_FORGE_MOD_ID = "kotlinforforge"

        val LOGGER = LogUtils.getLogger()
    }
}

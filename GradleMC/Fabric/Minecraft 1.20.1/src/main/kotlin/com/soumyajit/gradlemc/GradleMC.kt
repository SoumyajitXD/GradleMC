package com.soumyajit.gradlemc

import com.soumyajit.gradlemc.command.GradleMcCommands
import com.soumyajit.gradlemc.config.GradleMcConfig
import com.soumyajit.gradlemc.diagnostics.OperatingSystemInfoProvider
import com.soumyajit.gradlemc.network.GradleMcNetwork
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import com.soumyajit.gradlemc.performance.PerformanceService
import org.slf4j.LoggerFactory

/** Physical-server-safe entrypoint. Client wiring belongs exclusively to GradleMcClient. */
object GradleMC : ModInitializer {
    const val MOD_ID = "gradlemc"
    const val VERSION = "1.1.0"
    const val MINECRAFT_VERSION = "1.20.1"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        OperatingSystemInfoProvider.initialize()
        GradleMcConfig.load()
        GradleMcNetwork.registerCommon()
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> GradleMcCommands.register(dispatcher) }
        ServerTickEvents.START_SERVER_TICK.register { PerformanceService.recordServerTickStart() }
        ServerTickEvents.END_SERVER_TICK.register { PerformanceService.recordServerTickEnd()?.let(LOGGER::info) }
        ServerLifecycleEvents.SERVER_STOPPED.register { if (PerformanceService.resetServerTiming()) LOGGER.info("Cancelled GradleMC server performance sample during server shutdown") }
        LOGGER.info("GradleMC {} Fabric bootstrap loaded", VERSION)
    }
}

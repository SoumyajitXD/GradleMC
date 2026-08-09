package com.soumyajit.gradlemc.client

import com.soumyajit.gradlemc.GradleMC
import com.soumyajit.gradlemc.client.gui.GradleMcClientScreens
import com.soumyajit.gradlemc.client.input.GradleMCKeyMappings
import com.soumyajit.gradlemc.client.overlay.GradleMcStatsOverlay
import com.soumyajit.gradlemc.config.ForgeGradleMCConfig
import com.soumyajit.gradlemc.performance.PerformanceService
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.eventbus.api.SubscribeEvent
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

/** Explicit client-only event registration, invoked through the sided bootstrap. */
object GradleMcClientBootstrap {
    private val registrationGate = ClientRegistrationGate()

    @JvmStatic
    fun bootstrap() {
        register(KotlinModLoadingContext.get().getKEventBus())
    }

    @JvmStatic
    fun register(modEventBus: IEventBus) {
        val registered = registrationGate.registerOnce {
            if (!GradleMcGuiBridge.registerOpener(GradleMcClientScreens::requestOpen)) {
                GradleMC.LOGGER.warn("GradleMC GUI opener was already registered; keeping the first opener")
            }
            modEventBus.register(ClientModEvents::class.java)
            MinecraftForge.EVENT_BUS.register(ClientForgeEvents::class.java)
        }
        if (registered) {
            GradleMC.LOGGER.info("GradleMC client bootstrap registered key, tick, overlay, and GUI listeners")
        } else {
            GradleMC.LOGGER.warn("Prevented duplicate GradleMC client event registration")
        }
    }
}

object ClientModEvents {
    @SubscribeEvent
    @JvmStatic
    fun registerKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(GradleMCKeyMappings.openGui)
        event.register(GradleMCKeyMappings.toggleOverlay)
        GradleMC.LOGGER.info("GradleMC key mappings registered (GUI default: G)")
    }

    @SubscribeEvent
    @JvmStatic
    fun registerGuiOverlays(event: RegisterGuiOverlaysEvent) {
        event.registerAboveAll("stats_overlay", GradleMcStatsOverlay::render)
    }
}

object ClientForgeEvents {
    @SubscribeEvent
    @JvmStatic
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        GradleMcClientScreens.onClientTick()
        while (GradleMCKeyMappings.openGui.consumeClick()) {
            GradleMC.LOGGER.info("GradleMC GUI key consumed")
            if (GradleMcGuiBridge.requestOpen(GuiOpenOrigin.KEY) == GuiBridgeResult.NO_OPENER) {
                GradleMC.LOGGER.warn("GradleMC GUI key request skipped: no client opener is registered")
            }
        }
        while (GradleMCKeyMappings.toggleOverlay.consumeClick()) {
            val enabled = !ForgeGradleMCConfig.overlayEnabled.get()
            if (ForgeGradleMCConfig.setOverlayEnabled(enabled)) {
                GradleMcStatsOverlay.onSettingsChanged()
                GradleMC.LOGGER.info("GradleMC overlay {}", if (enabled) "enabled" else "disabled")
            } else {
                GradleMC.LOGGER.error("Unable to persist the GradleMC overlay setting")
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onClientLoggingIn(event: ClientPlayerNetworkEvent.LoggingIn) {
        PerformanceService.resetClientSession()
        GradleMcClientScreens.resetSession()
        GradleMC.LOGGER.info("GradleMC client diagnostics state reset for a new world session")
    }

    @SubscribeEvent
    @JvmStatic
    fun onClientLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        PerformanceService.resetClientSession()
        GradleMcClientScreens.resetSession()
        GradleMC.LOGGER.info("GradleMC client diagnostics state reset on world disconnect")
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderGui(event: RenderGuiEvent.Post) {
        val minecraft = Minecraft.getInstance()
        if (minecraft.level != null && minecraft.player != null && minecraft.isWindowActive && !minecraft.isPaused) {
            PerformanceService.recordRenderedFrame(System.nanoTime())
        } else {
            PerformanceService.resetFrameTiming()
        }
    }
}

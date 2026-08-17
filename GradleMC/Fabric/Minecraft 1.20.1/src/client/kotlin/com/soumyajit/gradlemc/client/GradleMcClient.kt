package com.soumyajit.gradlemc.client

import com.mojang.blaze3d.platform.InputConstants
import com.soumyajit.gradlemc.GradleMC
import com.soumyajit.gradlemc.config.GradleMcConfig
import com.soumyajit.gradlemc.network.GradleMcNetwork
import com.soumyajit.gradlemc.performance.PerformanceService
import com.soumyajit.gradlemc.performance.FpsTestService
import com.soumyajit.gradlemc.client.overlay.GradleMcStatsOverlay
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW

object GradleMcClient : ClientModInitializer {
    private lateinit var openKey: KeyMapping

    override fun onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(KeyMapping("key.gradlemc.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.gradlemc"))
        GradleMcOverlayClientCommands.register()
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openKey.consumeClick()) GradleMcClientScreens.requestOpen(client, GuiOpenOrigin.KEY)
            GradleMcClientScreens.onClientTick(client)
        }
        ClientPlayNetworking.registerGlobalReceiver(GradleMcNetwork.OPEN_GUI) { client, _, _, _ ->
            GradleMcClientScreens.requestOpen(client, GuiOpenOrigin.SERVER_COMMAND)
        }
        ClientPlayNetworking.registerGlobalReceiver(GradleMcNetwork.FPS_TEST_ACTION) { client, _, payload, _ ->
            val seconds = payload.readVarInt()
            client.execute { if (seconds == 0) FpsTestService.stop() else FpsTestService.start(seconds) }
        }
        // The HUD callback is our post-render frame producer.  It must not depend on the
        // overlay being enabled: explicit FPS tests are useful with the (default) hidden HUD.
        HudRenderCallback.EVENT.register(::onHudRendered)
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> PerformanceService.resetClientSession(); GradleMcClientScreens.resetSession() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> PerformanceService.resetClientSession(); GradleMcClientScreens.resetSession() }
        ClientLifecycleEvents.CLIENT_STOPPING.register { _ -> PerformanceService.resetClientSession(); GradleMcClientScreens.resetSession() }
        GradleMC.LOGGER.info("GradleMC client integration registered")
    }

    private fun onHudRendered(graphics: GuiGraphics, partialTick: Float) {
        val client = Minecraft.getInstance()
        if (client.level != null && client.player != null && client.isWindowActive && !client.isPaused) {
            PerformanceService.recordRenderedFrame()
        } else {
            PerformanceService.resetFrameTiming()
        }
        GradleMcStatsOverlay.render(graphics, partialTick)
    }
}

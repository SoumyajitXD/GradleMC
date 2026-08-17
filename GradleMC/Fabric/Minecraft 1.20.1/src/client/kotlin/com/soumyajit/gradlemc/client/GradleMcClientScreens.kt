package com.soumyajit.gradlemc.client

import com.soumyajit.gradlemc.GradleMC
import com.soumyajit.gradlemc.config.GradleMcConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import java.util.concurrent.atomic.AtomicBoolean

/** The only client-side owner of screen activation. Common commands only send the existing S2C packet. */
internal object GradleMcClientScreens {
    private val pending = AtomicBoolean(false)
    private var deferredCommandOpen = false
    fun requestOpen(client: Minecraft, origin: GuiOpenOrigin) {
        if (!pending.compareAndSet(false, true)) return
        client.execute { try { open(client, origin, true) } finally { pending.set(false) } }
    }
    fun onClientTick(client: Minecraft) {
        if (!deferredCommandOpen) return
        when (screenKind(client.screen)) {
            ActiveScreenKind.CHAT -> Unit
            ActiveScreenKind.NONE -> { deferredCommandOpen = false; open(client, GuiOpenOrigin.SERVER_COMMAND, false) }
            else -> deferredCommandOpen = false
        }
    }
    fun resetSession() { deferredCommandOpen = false; pending.set(false) }
    private fun open(client: Minecraft, origin: GuiOpenOrigin, allowChatDeferral: Boolean) {
        when (GuiOpenPolicy.decide(origin, GradleMcConfig.current().keyBindingEnabled, client.level != null, client.player != null, screenKind(client.screen))) {
            GuiOpenDecision.OPEN -> client.setScreen(GradleMcDiagnosticsScreen())
            GuiOpenDecision.DEFER_CHAT -> if (allowChatDeferral) deferredCommandOpen = true
            else -> Unit
        }
    }
    private fun screenKind(screen: Screen?): ActiveScreenKind = when (screen) {
        null -> ActiveScreenKind.NONE; is ChatScreen -> ActiveScreenKind.CHAT; is GradleMcDiagnosticsScreen -> ActiveScreenKind.GRADLE_MC; else -> ActiveScreenKind.OTHER
    }
}

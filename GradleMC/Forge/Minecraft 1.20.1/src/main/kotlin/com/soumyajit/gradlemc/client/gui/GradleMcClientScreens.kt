package com.soumyajit.gradlemc.client.gui

import com.soumyajit.gradlemc.GradleMC
import com.soumyajit.gradlemc.client.GuiOpenOrigin
import com.soumyajit.gradlemc.config.ForgeGradleMCConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen

/** The sole physical-client owner of GradleMC diagnostics screen opening. */
object GradleMcClientScreens {
    private val openWorkGate = GuiOpenWorkGate()
    private var deferredCommandOpen = false
    private var mainScreenWasActive = false

    @JvmStatic
    fun requestOpen(origin: GuiOpenOrigin) {
        GradleMC.LOGGER.debug("GradleMC GUI open requested (origin={})", origin.name.lowercase())
        val minecraft = Minecraft.getInstance()
        if (!openWorkGate.submit(minecraft::execute, Runnable {
                openMainScreenOnClientThread(minecraft, origin, allowChatDeferral = true)
            })) {
            GradleMC.LOGGER.debug("GradleMC GUI request coalesced: client open work is already pending")
        }
    }

    /** Called once at the end of each client tick; it logs only state transitions. */
    fun onClientTick() {
        val minecraft = Minecraft.getInstance()
        observeClose(minecraft)
        if (!deferredCommandOpen) return

        when (screenKind(minecraft.screen)) {
            ActiveScreenKind.CHAT -> return
            ActiveScreenKind.NONE -> {
                deferredCommandOpen = false
                GradleMC.LOGGER.info("Running deferred GradleMC GUI open after chat closed")
                openMainScreenOnClientThread(minecraft, GuiOpenOrigin.SERVER_COMMAND, allowChatDeferral = false)
            }
            else -> {
                deferredCommandOpen = false
                GradleMC.LOGGER.info(
                    "GradleMC GUI request skipped: chat was replaced by {}",
                    screenName(minecraft.screen),
                )
            }
        }
    }

    fun resetSession() {
        deferredCommandOpen = false
        mainScreenWasActive = false
    }

    private fun openMainScreenOnClientThread(
        minecraft: Minecraft,
        origin: GuiOpenOrigin,
        allowChatDeferral: Boolean,
    ) {
        GradleMC.LOGGER.debug("Running GradleMC GUI open on client thread (origin={})", origin.name.lowercase())
        val decision = GuiOpenPolicy.decide(
            origin = origin,
            keybindEnabled = ForgeGradleMCConfig.guiKeybindEnabled.get(),
            hasLevel = minecraft.level != null,
            hasPlayer = minecraft.player != null,
            activeScreen = screenKind(minecraft.screen),
        )
        if (decision == GuiOpenDecision.DEFER_CHAT && allowChatDeferral) {
            if (!deferredCommandOpen) {
                deferredCommandOpen = true
                GradleMC.LOGGER.info("GradleMC GUI command request deferred until chat closes")
            }
            return
        }
        if (decision != GuiOpenDecision.OPEN) {
            GradleMC.LOGGER.info("GradleMC GUI request skipped: {}", decision.logReason)
            return
        }

        try {
            val screen = GradleMcDiagnosticsScreen()
            GradleMC.LOGGER.debug("Minecraft.setScreen(...) called with {}", screen.javaClass.name)
            minecraft.setScreen(screen)
            val active = minecraft.screen
            if (active === screen) {
                mainScreenWasActive = true
                GradleMC.LOGGER.info("Active screen confirmed: {}", active.javaClass.name)
            } else {
                GradleMC.LOGGER.error(
                    "GradleMC screen activation failed; active screen is {}",
                    screenName(active),
                )
            }
        } catch (failure: RuntimeException) {
            GradleMC.LOGGER.error("Unable to construct or open the GradleMC diagnostics screen", failure)
            throw failure
        }
    }

    private fun observeClose(minecraft: Minecraft) {
        val mainScreenIsActive = minecraft.screen is GradleMcDiagnosticsScreen
        if (mainScreenWasActive && !mainScreenIsActive) {
            GradleMC.LOGGER.info("GradleMC diagnostics screen closed; active screen is {}", screenName(minecraft.screen))
        }
        mainScreenWasActive = mainScreenIsActive
    }

    private fun screenKind(screen: Screen?): ActiveScreenKind = when (screen) {
        null -> ActiveScreenKind.NONE
        is GradleMcDiagnosticsScreen -> ActiveScreenKind.GRADLE_MC
        is ChatScreen -> ActiveScreenKind.CHAT
        else -> ActiveScreenKind.OTHER
    }

    private fun screenName(screen: Screen?): String = screen?.javaClass?.name ?: "none"
}

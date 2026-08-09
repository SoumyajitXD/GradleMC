package com.soumyajit.gradlemc.client

/** Identifies the user action that requested the client diagnostics screen. */
enum class GuiOpenOrigin {
    KEY,
    SERVER_COMMAND,
}

enum class GuiBridgeResult {
    ACCEPTED,
    NO_OPENER,
}

fun interface GradleMcGuiOpener {
    fun requestOpen(origin: GuiOpenOrigin)
}

/**
 * Small common-safe dispatcher. It deliberately contains no Minecraft client types, so the
 * server-to-client packet can refer to it without linking a screen class on dedicated servers.
 */
internal class GuiOpenBridge {
    @Volatile
    private var opener: GradleMcGuiOpener? = null

    @Synchronized
    fun registerOpener(candidate: GradleMcGuiOpener): Boolean {
        if (opener != null) return false
        opener = candidate
        return true
    }

    fun requestOpen(origin: GuiOpenOrigin): GuiBridgeResult {
        val activeOpener = opener ?: return GuiBridgeResult.NO_OPENER
        activeOpener.requestOpen(origin)
        return GuiBridgeResult.ACCEPTED
    }
}

/** Common-safe seam installed explicitly by the physical-client bootstrap. */
object GradleMcGuiBridge {
    private val bridge = GuiOpenBridge()

    fun registerOpener(opener: GradleMcGuiOpener): Boolean = bridge.registerOpener(opener)

    fun requestOpen(origin: GuiOpenOrigin): GuiBridgeResult = bridge.requestOpen(origin)
}

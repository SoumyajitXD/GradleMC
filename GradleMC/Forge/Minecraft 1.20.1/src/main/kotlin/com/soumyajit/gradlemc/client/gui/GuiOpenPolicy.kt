package com.soumyajit.gradlemc.client.gui

import com.soumyajit.gradlemc.client.GuiOpenOrigin

enum class ActiveScreenKind {
    NONE,
    CHAT,
    GRADLE_MC,
    OTHER,
}

enum class GuiOpenDecision(val logReason: String) {
    OPEN("eligible"),
    DEFER_CHAT("chat is closing"),
    SKIP_KEYBIND_DISABLED("GUI keybind is disabled"),
    SKIP_NO_LEVEL("no client level"),
    SKIP_NO_PLAYER("no client player"),
    SKIP_ALREADY_OPEN("GradleMC diagnostics is already open"),
    SKIP_SCREEN_ACTIVE("another screen is active"),
}

/** Pure policy shared by key and command request paths. */
object GuiOpenPolicy {
    fun decide(
        origin: GuiOpenOrigin,
        keybindEnabled: Boolean,
        hasLevel: Boolean,
        hasPlayer: Boolean,
        activeScreen: ActiveScreenKind,
    ): GuiOpenDecision {
        if (origin == GuiOpenOrigin.KEY && !keybindEnabled) return GuiOpenDecision.SKIP_KEYBIND_DISABLED
        if (!hasLevel) return GuiOpenDecision.SKIP_NO_LEVEL
        if (!hasPlayer) return GuiOpenDecision.SKIP_NO_PLAYER
        return when (activeScreen) {
            ActiveScreenKind.NONE -> GuiOpenDecision.OPEN
            ActiveScreenKind.GRADLE_MC -> GuiOpenDecision.SKIP_ALREADY_OPEN
            ActiveScreenKind.CHAT -> if (origin == GuiOpenOrigin.SERVER_COMMAND) {
                GuiOpenDecision.DEFER_CHAT
            } else {
                GuiOpenDecision.SKIP_SCREEN_ACTIVE
            }
            ActiveScreenKind.OTHER -> GuiOpenDecision.SKIP_SCREEN_ACTIVE
        }
    }
}

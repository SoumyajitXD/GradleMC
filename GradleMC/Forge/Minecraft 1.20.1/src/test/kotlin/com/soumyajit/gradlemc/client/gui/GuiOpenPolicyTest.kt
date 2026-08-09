package com.soumyajit.gradlemc.client.gui

import com.soumyajit.gradlemc.client.GuiOpenOrigin
import kotlin.test.Test
import kotlin.test.assertEquals

class GuiOpenPolicyTest {
    @Test
    fun `key requests require enabled keybind world player and no screen`() {
        assertEquals(GuiOpenDecision.SKIP_KEYBIND_DISABLED, decide(GuiOpenOrigin.KEY, enabled = false))
        assertEquals(GuiOpenDecision.SKIP_NO_LEVEL, decide(GuiOpenOrigin.KEY, hasLevel = false))
        assertEquals(GuiOpenDecision.SKIP_NO_PLAYER, decide(GuiOpenOrigin.KEY, hasPlayer = false))
        assertEquals(GuiOpenDecision.SKIP_SCREEN_ACTIVE, decide(GuiOpenOrigin.KEY, screen = ActiveScreenKind.CHAT))
        assertEquals(GuiOpenDecision.SKIP_SCREEN_ACTIVE, decide(GuiOpenOrigin.KEY, screen = ActiveScreenKind.OTHER))
        assertEquals(GuiOpenDecision.OPEN, decide(GuiOpenOrigin.KEY))
    }

    @Test
    fun `command defers only chat and never replaces another screen`() {
        assertEquals(GuiOpenDecision.DEFER_CHAT, decide(GuiOpenOrigin.SERVER_COMMAND, screen = ActiveScreenKind.CHAT))
        assertEquals(GuiOpenDecision.SKIP_SCREEN_ACTIVE, decide(GuiOpenOrigin.SERVER_COMMAND, screen = ActiveScreenKind.OTHER))
        assertEquals(GuiOpenDecision.OPEN, decide(GuiOpenOrigin.SERVER_COMMAND, enabled = false))
    }

    @Test
    fun `already open diagnostics screen is idempotently skipped for every origin`() {
        GuiOpenOrigin.entries.forEach { origin ->
            assertEquals(GuiOpenDecision.SKIP_ALREADY_OPEN, decide(origin, screen = ActiveScreenKind.GRADLE_MC))
        }
    }

    private fun decide(
        origin: GuiOpenOrigin,
        enabled: Boolean = true,
        hasLevel: Boolean = true,
        hasPlayer: Boolean = true,
        screen: ActiveScreenKind = ActiveScreenKind.NONE,
    ): GuiOpenDecision = GuiOpenPolicy.decide(origin, enabled, hasLevel, hasPlayer, screen)
}

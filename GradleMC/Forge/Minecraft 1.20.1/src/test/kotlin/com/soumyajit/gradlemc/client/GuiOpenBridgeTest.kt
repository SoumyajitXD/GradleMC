package com.soumyajit.gradlemc.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuiOpenBridgeTest {
    @Test
    fun `bridge reports absence then delegates to its first registered opener`() {
        val bridge = GuiOpenBridge()
        val origins = mutableListOf<GuiOpenOrigin>()

        assertEquals(GuiBridgeResult.NO_OPENER, bridge.requestOpen(GuiOpenOrigin.KEY))
        assertTrue(bridge.registerOpener(GradleMcGuiOpener(origins::add)))
        assertFalse(bridge.registerOpener(GradleMcGuiOpener { error("duplicate opener must not run") }))

        assertEquals(GuiBridgeResult.ACCEPTED, bridge.requestOpen(GuiOpenOrigin.SERVER_COMMAND))
        assertEquals(listOf(GuiOpenOrigin.SERVER_COMMAND), origins)
    }
}

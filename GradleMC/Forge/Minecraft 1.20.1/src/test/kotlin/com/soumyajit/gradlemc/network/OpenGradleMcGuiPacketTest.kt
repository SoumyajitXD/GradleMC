package com.soumyajit.gradlemc.network

import net.minecraftforge.network.NetworkDirection
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenGradleMcGuiPacketTest {
    @Test
    fun `GUI packet accepts only server to client play direction`() {
        assertTrue(OpenGradleMcGuiPacket.isEligibleDirection(NetworkDirection.PLAY_TO_CLIENT))
        assertFalse(OpenGradleMcGuiPacket.isEligibleDirection(NetworkDirection.PLAY_TO_SERVER))
        assertFalse(OpenGradleMcGuiPacket.isEligibleDirection(NetworkDirection.LOGIN_TO_CLIENT))
        assertFalse(OpenGradleMcGuiPacket.isEligibleDirection(NetworkDirection.LOGIN_TO_SERVER))
    }
}

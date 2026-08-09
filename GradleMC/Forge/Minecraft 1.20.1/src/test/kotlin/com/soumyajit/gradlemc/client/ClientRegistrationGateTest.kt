package com.soumyajit.gradlemc.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientRegistrationGateTest {
    @Test
    fun `successful registration runs exactly once`() {
        val gate = ClientRegistrationGate()
        var registrations = 0

        assertTrue(gate.registerOnce { registrations++ })
        assertFalse(gate.registerOnce { registrations++ })
        assertEquals(1, registrations)
    }

    @Test
    fun `failed registration can be retried`() {
        val gate = ClientRegistrationGate()
        assertFailsWith<IllegalStateException> { gate.registerOnce { error("registration failed") } }
        assertTrue(gate.registerOnce { })
        assertFalse(gate.registerOnce { })
    }
}

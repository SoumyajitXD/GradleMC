package com.soumyajit.gradlemc.client.gui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GuiOpenWorkGateTest {
    @Test
    fun `coalesces requests while one client task is queued and reopens after completion`() {
        val gate = GuiOpenWorkGate()
        val queued = ArrayDeque<Runnable>()
        var runs = 0

        assertTrue(gate.submit(queued::addLast, Runnable { runs++ }))
        assertFalse(gate.submit(queued::addLast, Runnable { error("must be coalesced") }))
        assertEquals(1, queued.size)

        queued.removeFirst().run()
        assertEquals(1, runs)
        assertTrue(gate.submit(queued::addLast, Runnable { runs++ }))
        queued.removeFirst().run()
        assertEquals(2, runs)
    }

    @Test
    fun `recovers after scheduling or client work fails`() {
        val gate = GuiOpenWorkGate()
        assertFailsWith<IllegalStateException> {
            gate.submit({ error("scheduler stopped") }, Runnable {})
        }

        var queued: Runnable? = null
        assertTrue(gate.submit({ queued = it }, Runnable { error("screen construction failed") }))
        assertFailsWith<IllegalStateException> { queued!!.run() }

        assertTrue(gate.submit({ queued = it }, Runnable {}))
        queued!!.run()
    }
}

package com.soumyajit.gradlemc.client.gui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GradleMcGuiLayoutTest {
    @Test
    fun `normal window keeps content inside the panel`() {
        val layout = GradleMcGuiLayout.calculate(854, 480)

        assertTrue(layout.left >= 0)
        assertTrue(layout.top >= 0)
        assertTrue(layout.contentLeft > layout.left)
        assertTrue(layout.contentLeft + layout.contentWidth <= layout.right)
        assertTrue(layout.contentTop >= layout.headerBottom)
        assertTrue(layout.contentBottom <= layout.footerTop)
    }

    @Test
    fun `tiny gui scale remains nonnegative and drawable`() {
        val layout = GradleMcGuiLayout.calculate(100, 60)

        assertTrue(layout.right > layout.left)
        assertTrue(layout.bottom > layout.top)
        assertTrue(layout.contentWidth >= 1)
        assertTrue(layout.contentBottom >= layout.contentTop - 14)
    }

    @Test
    fun `crash-report viewport fits every navigation tab above the footer`() {
        val layout = GradleMcGuiLayout.calculate(480, 265)
        val tabs = layout.navigationTabs(8)

        assertEquals(8, tabs.size)
        assertTrue(tabs.all { it.height > 0 && it.y >= layout.contentTop && it.y + it.height <= layout.footerTop })
    }

    @Test
    fun `small common viewport keeps all navigation tabs out of the footer`() {
        val layout = GradleMcGuiLayout.calculate(320, 240)
        val tabs = layout.navigationTabs(8)

        assertEquals(8, tabs.size)
        assertTrue(tabs.zipWithNext().all { (first, second) -> first.y + first.height <= second.y })
        assertTrue(tabs.last().y + tabs.last().height <= layout.footerTop)
    }

    @Test
    fun `large viewport uses a compact centered panel`() {
        val layout = GradleMcGuiLayout.calculate(960, 540)

        assertEquals(780, layout.right - layout.left)
        assertEquals(430, layout.bottom - layout.top)
        assertEquals((960 - 780) / 2, layout.left)
        assertEquals((540 - 430) / 2, layout.top)
    }
}

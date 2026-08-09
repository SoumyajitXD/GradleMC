package com.soumyajit.gradlemc.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticsConfigDefaultsTest {
    @Test
    fun `diagnostics defaults keep the overlay hidden and use balanced detail`() {
        val defaults = GradleMCConfigSnapshot.defaults()

        assertEquals("reports", defaults.reportDirectoryName)
        assertFalse(defaults.overlayEnabled)
        assertFalse(defaults.overlayShowTitle)
        assertTrue(defaults.overlayShowFps)
        assertFalse(defaults.overlayShowAverageFps)
        assertEquals(60, defaults.overlaySamplingWindowSeconds)
        assertTrue(defaults.guiKeybindEnabled)
        assertEquals("balanced", defaults.performanceMode)
        assertEquals("showOverlayTitle", ForgeGradleMCConfig.OVERLAY_SHOW_TITLE_KEY)
        assertEquals("overlaySamplingWindowSeconds", ForgeGradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS_KEY)
    }

    @Test
    fun `only known persisted performance modes are accepted`() {
        assertTrue(GradleMCConfigSnapshot.isValidPerformanceMode("low_impact"))
        assertTrue(GradleMCConfigSnapshot.isValidPerformanceMode("BALANCED"))
        assertTrue(GradleMCConfigSnapshot.isValidPerformanceMode("detailed"))
        assertFalse(GradleMCConfigSnapshot.isValidPerformanceMode("fast"))
    }

    @Test
    fun `rolling window accepts only authoritative donor values and recovers to default`() {
        assertTrue(GradleMCConfigSnapshot.isValidOverlaySamplingWindowSeconds(30))
        assertTrue(GradleMCConfigSnapshot.isValidOverlaySamplingWindowSeconds(60))
        assertTrue(GradleMCConfigSnapshot.isValidOverlaySamplingWindowSeconds(120))
        assertFalse(GradleMCConfigSnapshot.isValidOverlaySamplingWindowSeconds(29))
        assertFalse(GradleMCConfigSnapshot.isValidOverlaySamplingWindowSeconds(61))
        assertEquals(60, GradleMCConfigSnapshot.normalizedOverlaySamplingWindowSeconds(61))
    }

    @Test
    fun `persistence helper saves a new value and rolls back a failed save`() {
        var value = false
        assertTrue(persistConfigValue({ value }, { value = it }, {}, true))
        assertTrue(value)

        assertFalse(persistConfigValue({ value }, { value = it }, { error("disk unavailable") }, false))
        assertTrue(value)
    }
}

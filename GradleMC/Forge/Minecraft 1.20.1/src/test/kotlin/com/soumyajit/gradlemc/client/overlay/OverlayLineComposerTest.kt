package com.soumyajit.gradlemc.client.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class OverlayLineComposerTest {
    @Test
    fun `title current fps and average fps are independently configurable`() {
        assertEquals(emptyList(), compose(title = false, current = false, average = false))
        assertEquals(listOf("GradleMC"), compose(title = true, current = false, average = false))
        assertEquals(listOf("FPS: 60"), compose(title = false, current = true, average = false))
        assertEquals(listOf("Average FPS: 58"), compose(title = false, current = false, average = true))
        assertEquals(
            listOf("GradleMC", "FPS: 60", "Average FPS: 58"),
            compose(title = true, current = true, average = true),
        )
    }

    @Test
    fun `unavailable values warm up without fabricating fps`() {
        assertEquals(
            listOf("FPS: warming up", "Average FPS: warming up"),
            OverlayLineComposer.compose(false, true, true, null, Double.NaN),
        )
    }

    private fun compose(title: Boolean, current: Boolean, average: Boolean): List<String> =
        OverlayLineComposer.compose(title, current, average, 60.0, 58.0)
}

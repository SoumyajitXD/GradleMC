package com.soumyajit.gradlemc.client.overlay

import java.util.Locale

/** Pure overlay text composition; each visible item is controlled independently. */
internal object OverlayLineComposer {
    fun compose(
        showTitle: Boolean,
        showCurrentFps: Boolean,
        showAverageFps: Boolean,
        currentFps: Double?,
        averageFps: Double?,
    ): List<String> = buildList(3) {
        if (showTitle) add("GradleMC")
        if (showCurrentFps) add("FPS: ${format(currentFps)}")
        if (showAverageFps) add("Average FPS: ${format(averageFps)}")
    }

    private fun format(value: Double?): String =
        if (value == null || !value.isFinite()) {
            "warming up"
        } else {
            String.format(Locale.ROOT, "%.0f", value.coerceAtLeast(0.0))
        }
}

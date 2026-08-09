package com.soumyajit.gradlemc.config

/** Stable defaults for the small, local report-output configuration slice. */
object GradleMCDefaults {
    const val REPORT_DIRECTORY_NAME = "reports"
    const val MAX_REPORT_DIRECTORY_NAME_LENGTH = 64
    const val OVERLAY_ENABLED = false
    const val OVERLAY_SHOW_TITLE = false
    const val OVERLAY_SHOW_FPS = true
    const val OVERLAY_SHOW_AVERAGE_FPS = false
    const val OVERLAY_SAMPLING_WINDOW_SECONDS = 60
    const val GUI_KEYBIND_ENABLED = true
    const val PERFORMANCE_MODE = "balanced"
}

package com.soumyajit.gradlemc.config

import java.util.Locale

private val windowsReservedNames = buildSet {
    addAll(listOf("CON", "PRN", "AUX", "NUL"))
    (1..9).forEach { number ->
        add("COM$number")
        add("LPT$number")
    }
}

/**
 * Pure, immutable configuration consumed by future report-output code.
 * Forge values are deliberately kept in the adapter, not in this snapshot.
 */
data class GradleMCConfigSnapshot(
    val reportDirectoryName: String,
    val overlayEnabled: Boolean = GradleMCDefaults.OVERLAY_ENABLED,
    val overlayShowTitle: Boolean = GradleMCDefaults.OVERLAY_SHOW_TITLE,
    val overlayShowFps: Boolean = GradleMCDefaults.OVERLAY_SHOW_FPS,
    val overlayShowAverageFps: Boolean = GradleMCDefaults.OVERLAY_SHOW_AVERAGE_FPS,
    val overlaySamplingWindowSeconds: Int = GradleMCDefaults.OVERLAY_SAMPLING_WINDOW_SECONDS,
    val guiKeybindEnabled: Boolean = GradleMCDefaults.GUI_KEYBIND_ENABLED,
    val performanceMode: String = GradleMCDefaults.PERFORMANCE_MODE,
) {
    init {
        require(isValidReportDirectoryName(reportDirectoryName)) {
            "reportDirectoryName must be a safe single directory name"
        }
    }

    companion object {
        fun defaults() = GradleMCConfigSnapshot(
            reportDirectoryName = GradleMCDefaults.REPORT_DIRECTORY_NAME,
        )

        fun isValidReportDirectoryName(value: String): Boolean =
            value.length in 1..GradleMCDefaults.MAX_REPORT_DIRECTORY_NAME_LENGTH &&
                value != "." && value != ".." &&
                !value.endsWith(' ') && !value.endsWith('.') &&
                value.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) &&
                value.substringBefore('.').uppercase(Locale.ROOT) !in windowsReservedNames

        fun isValidPerformanceMode(value: String): Boolean = when (value.lowercase(Locale.ROOT)) {
            "low_impact", "balanced", "detailed" -> true
            else -> false
        }

        fun isValidOverlaySamplingWindowSeconds(value: Int): Boolean = value == 30 || value == 60 || value == 120

        fun normalizedOverlaySamplingWindowSeconds(value: Int): Int =
            value.takeIf(::isValidOverlaySamplingWindowSeconds)
                ?: GradleMCDefaults.OVERLAY_SAMPLING_WINDOW_SECONDS
    }
}

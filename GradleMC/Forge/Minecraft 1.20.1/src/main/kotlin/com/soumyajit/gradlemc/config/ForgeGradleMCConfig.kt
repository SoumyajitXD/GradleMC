package com.soumyajit.gradlemc.config

import net.minecraftforge.common.ForgeConfigSpec

/** The only Forge-facing part of this configuration slice. */
object ForgeGradleMCConfig {
    const val FILE_NAME = "gradlemc-common.toml"
    const val REPORT_DIRECTORY_NAME_KEY = "reportDirectoryName"
    const val OVERLAY_ENABLED_KEY = "overlayEnabled"
    const val OVERLAY_SHOW_TITLE_KEY = "showOverlayTitle"
    const val OVERLAY_SHOW_FPS_KEY = "overlayShowFps"
    const val OVERLAY_SHOW_AVERAGE_FPS_KEY = "overlayShowAverageFps"
    const val OVERLAY_SAMPLING_WINDOW_SECONDS_KEY = "overlaySamplingWindowSeconds"
    const val GUI_KEYBIND_ENABLED_KEY = "guiKeybindEnabled"
    const val PERFORMANCE_MODE_KEY = "performanceMode"

    private val builder = ForgeConfigSpec.Builder()

    val reportDirectoryName: ForgeConfigSpec.ConfigValue<String>
    val overlayEnabled: ForgeConfigSpec.BooleanValue
    val overlayShowTitle: ForgeConfigSpec.BooleanValue
    val overlayShowFps: ForgeConfigSpec.BooleanValue
    val overlayShowAverageFps: ForgeConfigSpec.BooleanValue
    val overlaySamplingWindowSeconds: ForgeConfigSpec.ConfigValue<Int>
    val guiKeybindEnabled: ForgeConfigSpec.BooleanValue
    val performanceMode: ForgeConfigSpec.ConfigValue<String>
    val spec: ForgeConfigSpec

    init {
        builder.push("diagnostics")
        reportDirectoryName = builder
            .comment("Single directory name under the local GradleMC output root; path separators are not allowed.")
            .define(REPORT_DIRECTORY_NAME_KEY, GradleMCDefaults.REPORT_DIRECTORY_NAME) {
                it is String && GradleMCConfigSnapshot.isValidReportDirectoryName(it)
            }
        overlayEnabled = builder.comment("Show the lightweight GradleMC overlay. Disabled by default.")
            .define(OVERLAY_ENABLED_KEY, GradleMCDefaults.OVERLAY_ENABLED)
        overlayShowTitle = builder.comment("Displays the \"GradleMC\" heading above enabled overlay statistics. Disabled by default.")
            .define(OVERLAY_SHOW_TITLE_KEY, GradleMCDefaults.OVERLAY_SHOW_TITLE)
        overlayShowFps = builder.comment("Show current rendered FPS when the overlay is enabled.")
            .define(OVERLAY_SHOW_FPS_KEY, GradleMCDefaults.OVERLAY_SHOW_FPS)
        overlayShowAverageFps = builder.comment("Show average rendered FPS when the overlay is enabled.")
            .define(OVERLAY_SHOW_AVERAGE_FPS_KEY, GradleMCDefaults.OVERLAY_SHOW_AVERAGE_FPS)
        overlaySamplingWindowSeconds = builder
            .comment("Rolling FPS statistics window in seconds. Allowed values: 30, 60, 120.")
            .defineInList(
                OVERLAY_SAMPLING_WINDOW_SECONDS_KEY,
                GradleMCDefaults.OVERLAY_SAMPLING_WINDOW_SECONDS,
                listOf(30, 60, 120),
            )
        guiKeybindEnabled = builder.comment("Allow the configured GradleMC keybind to open diagnostics.")
            .define(GUI_KEYBIND_ENABLED_KEY, GradleMCDefaults.GUI_KEYBIND_ENABLED)
        performanceMode = builder.comment("Diagnostic detail policy: low_impact, balanced, or detailed.")
            .define(PERFORMANCE_MODE_KEY, GradleMCDefaults.PERFORMANCE_MODE) {
                it is String && GradleMCConfigSnapshot.isValidPerformanceMode(it)
            }
        builder.pop()
        spec = builder.build()
    }

    /** Call only after Forge has loaded this specification; no snapshot is cached. */
    fun snapshot(): GradleMCConfigSnapshot = GradleMCConfigSnapshot(
        reportDirectoryName = reportDirectoryName.get().takeIf(GradleMCConfigSnapshot::isValidReportDirectoryName)
            ?: GradleMCDefaults.REPORT_DIRECTORY_NAME,
        overlayEnabled = overlayEnabled.get(),
        overlayShowTitle = overlayShowTitle.get(),
        overlayShowFps = overlayShowFps.get(),
        overlayShowAverageFps = overlayShowAverageFps.get(),
        overlaySamplingWindowSeconds = GradleMCConfigSnapshot.normalizedOverlaySamplingWindowSeconds(
            overlaySamplingWindowSeconds.get(),
        ),
        guiKeybindEnabled = guiKeybindEnabled.get(),
        performanceMode = performanceMode.get().takeIf(GradleMCConfigSnapshot::isValidPerformanceMode)
            ?: GradleMCDefaults.PERFORMANCE_MODE,
    )

    fun setOverlayEnabled(value: Boolean): Boolean = persistBoolean(overlayEnabled, value)

    fun setOverlayShowTitle(value: Boolean): Boolean = persistBoolean(overlayShowTitle, value)

    fun setOverlayShowFps(value: Boolean): Boolean = persistBoolean(overlayShowFps, value)

    fun setOverlayShowAverageFps(value: Boolean): Boolean = persistBoolean(overlayShowAverageFps, value)

    fun setOverlaySamplingWindowSeconds(value: Int): Boolean {
        if (!GradleMCConfigSnapshot.isValidOverlaySamplingWindowSeconds(value)) return false
        return persistValue(overlaySamplingWindowSeconds, value)
    }

    private fun persistBoolean(setting: ForgeConfigSpec.BooleanValue, value: Boolean): Boolean {
        return persistValue(setting, value)
    }

    private fun <T> persistValue(setting: ForgeConfigSpec.ConfigValue<T>, value: T): Boolean = persistConfigValue(
        current = setting::get,
        set = setting::set,
        save = spec::save,
        value = value,
    )
}

internal fun <T> persistConfigValue(
    current: () -> T,
    set: (T) -> Unit,
    save: () -> Unit,
    value: T,
): Boolean {
    val previous = current()
    return try {
        set(value)
        save()
        true
    } catch (_: RuntimeException) {
        runCatching { set(previous) }
        false
    }
}

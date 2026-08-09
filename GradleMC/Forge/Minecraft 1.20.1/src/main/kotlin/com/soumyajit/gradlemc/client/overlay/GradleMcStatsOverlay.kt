package com.soumyajit.gradlemc.client.overlay

import com.soumyajit.gradlemc.config.ForgeGradleMCConfig
import com.soumyajit.gradlemc.performance.PerformanceService
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraftforge.client.gui.overlay.ForgeGui

/**
 * Deliberately small overlay backed by the real render-frame producer.  Text and width are
 * refreshed at a bounded interval instead of being rebuilt for every frame.
 */
object GradleMcStatsOverlay {
    private const val MARGIN = 6
    private const val PADDING = 4
    private const val TEXT = 0xFFEAF0F7.toInt()
    private const val BACKGROUND = 0xA010141C.toInt()
    private const val REFRESH_MILLIS = 500L

    private var lastRefreshMillis = 0L
    private var cachedLines: List<String> = emptyList()
    private var cachedWidth = 0

    fun render(gui: ForgeGui, graphics: GuiGraphics, partialTick: Float, screenWidth: Int, screenHeight: Int) {
        val minecraft = Minecraft.getInstance()
        val shouldRender = ForgeGradleMCConfig.overlayEnabled.get() &&
            minecraft.level != null && minecraft.player != null &&
            minecraft.screen == null && !minecraft.options.renderDebug
        val wantsFrames = shouldRender &&
            (ForgeGradleMCConfig.overlayShowFps.get() || ForgeGradleMCConfig.overlayShowAverageFps.get())
        PerformanceService.setOverlayFrameDemand(wantsFrames)
        if (!shouldRender) {
            clearCache()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastRefreshMillis >= REFRESH_MILLIS || cachedLines.isEmpty()) {
            val snapshot = PerformanceService.snapshot()
            cachedLines = OverlayLineComposer.compose(
                showTitle = ForgeGradleMCConfig.overlayShowTitle.get(),
                showCurrentFps = ForgeGradleMCConfig.overlayShowFps.get(),
                showAverageFps = ForgeGradleMCConfig.overlayShowAverageFps.get(),
                currentFps = snapshot.currentFps,
                averageFps = snapshot.averageFps,
            )
            cachedWidth = cachedLines.maxOfOrNull(minecraft.font::width) ?: 0
            lastRefreshMillis = now
        }
        if (cachedLines.isEmpty()) return

        val lineHeight = minecraft.font.lineHeight + 2
        val boxWidth = cachedWidth + PADDING * 2
        val boxHeight = cachedLines.size * lineHeight + PADDING * 2
        graphics.fill(MARGIN, MARGIN, MARGIN + boxWidth, MARGIN + boxHeight, BACKGROUND)
        var y = MARGIN + PADDING
        for (line in cachedLines) {
            graphics.drawString(minecraft.font, line, MARGIN + PADDING, y, TEXT, false)
            y += lineHeight
        }
    }

    fun onSettingsChanged() {
        PerformanceService.setOverlayFrameDemand(false)
        clearCache()
    }

    private fun clearCache() {
        cachedLines = emptyList()
        cachedWidth = 0
        lastRefreshMillis = 0L
    }
}

package com.soumyajit.gradlemc.client.gui

import com.soumyajit.gradlemc.client.overlay.GradleMcStatsOverlay
import com.soumyajit.gradlemc.config.ForgeGradleMCConfig
import com.soumyajit.gradlemc.diagnostics.DiagnosticsService
import com.soumyajit.gradlemc.diagnostics.ReportExportResult
import com.soumyajit.gradlemc.performance.FpsTestService
import com.soumyajit.gradlemc.performance.PerformanceMode
import com.soumyajit.gradlemc.performance.PerformanceService
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.Util
import java.util.Locale
import java.util.concurrent.CompletableFuture

/** Donor-style local diagnostics screen. Expensive work happens only on explicit actions or refresh. */
class GradleMcDiagnosticsScreen : Screen(Component.literal("GradleMC Diagnostics")) {
    private enum class Section(val label: String) {
        OVERVIEW("Overview"), QUICK_ACTIONS("Quick Actions"), ENVIRONMENT("Environment"), MEMORY("Memory"),
        PERFORMANCE("Performance"), STABILITY("Stability"), REPORTS("Reports"), SETTINGS("Settings"),
    }

    private var selected = Section.OVERVIEW
    private var status = "Ready"
    private var statusColor = MUTED
    private var lines: List<String> = emptyList()
    private var refreshCountdown = 0
    private var contentScroll = 0
    private var exportInProgress = false

    override fun init() {
        rebuildGui()
        refreshView()
    }

    override fun tick() {
        if (--refreshCountdown <= 0) {
            refreshView()
            refreshCountdown = 20
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics)
        val layout = GradleMcGuiLayout.calculate(width, height)
        graphics.fill(layout.left, layout.top, layout.right, layout.bottom, BACKGROUND)
        graphics.fill(layout.left, layout.top, layout.right, layout.headerBottom, PANEL_DARK)
        graphics.fill(layout.left, layout.headerBottom, layout.left + sidebarWidth(layout), layout.footerTop, PANEL_DARK)
        graphics.fill(layout.contentLeft - 8, layout.contentTop - 4, layout.right - 14, layout.footerTop - 6, PANEL)
        graphics.fill(layout.left, layout.footerTop, layout.right, layout.bottom, PANEL_DARK)
        drawBorder(graphics, layout)

        graphics.drawString(font, "GradleMC Diagnostics", layout.left + 14, layout.top + 13, TEXT, false)
        graphics.drawString(font, "Local rule-based diagnostics for Minecraft 1.20.1", layout.left + 14, layout.top + 29, MUTED, false)
        graphics.drawString(font, selected.label, layout.contentLeft, layout.contentTop, TEXT, false)
        var y = layout.contentTop + 17
        val lineLimit = layout.contentBottom - 4
        val wrapped = wrappedLines(layout)
        val visibleLineCount = ((lineLimit - y) / (font.lineHeight + 3) + 1).coerceAtLeast(0)
        contentScroll = contentScroll.coerceIn(0, (wrapped.size - visibleLineCount).coerceAtLeast(0))
        wrapped.drop(contentScroll).forEach { line ->
            if (y <= lineLimit) graphics.drawString(font, line, layout.contentLeft, y, MUTED, false)
            y += font.lineHeight + 3
        }
        graphics.drawString(font, status, layout.left + 14, layout.footerTop + 12, statusColor, false)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun onClose() {
        minecraft?.setScreen(null)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        val layout = GradleMcGuiLayout.calculate(width, height)
        if (mouseX < layout.contentLeft || mouseX > layout.right - 14 || mouseY < layout.contentTop || mouseY > layout.contentBottom) {
            return super.mouseScrolled(mouseX, mouseY, delta)
        }
        val step = when {
            delta > 0.0 -> -1
            delta < 0.0 -> 1
            else -> 0
        }
        if (step == 0) return false
        contentScroll = (contentScroll + step).coerceAtLeast(0)
        return true
    }

    private fun rebuildGui() {
        clearWidgets()
        val layout = GradleMcGuiLayout.calculate(width, height)
        val tabs = layout.navigationTabs(Section.entries.size)
        Section.entries.zip(tabs).forEach { (section, bounds) ->
            addRenderableWidget(Button.builder(Component.literal(section.label)) {
                selected = section
                contentScroll = 0
                rebuildGui()
                refreshView()
            }.bounds(bounds.x, bounds.y, bounds.width, bounds.height).build().also { it.active = section != selected })
        }

        addRenderableWidget(Button.builder(Component.literal("Refresh")) { refreshView() }
            .bounds(layout.right - 188, layout.footerTop + 7, 88, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Close")) { onClose() }
            .bounds(layout.right - 94, layout.footerTop + 7, 80, 20).build())
        buildSectionActions(layout)
    }

    private fun buildSectionActions(layout: GradleMcGuiLayout) {
        val x = layout.contentLeft
        // Content is rendered above actions.  Keeping the row origin derived from the fixed
        // summary count prevents buttons from ever sitting on top of labels.
        val y = layout.contentTop + 17 + summaryLineCount(selected) * (font.lineHeight + 3) + 4
        val columns = if (layout.contentWidth >= 390) 2 else 1
        val gap = 8
        val buttonWidth = ((layout.contentWidth - gap * (columns - 1)) / columns).coerceAtLeast(1)
        fun action(index: Int, label: String, run: () -> Unit) {
            val row = index / columns
            val column = index % columns
            val buttonY = y + row * 28
            if (buttonY + 20 > layout.contentBottom - 2) return
            addRenderableWidget(Button.builder(Component.literal(label)) { run() }
                .bounds(x + column * (buttonWidth + gap), buttonY, buttonWidth, 20).build())
        }
        when (selected) {
            Section.QUICK_ACTIONS -> {
                action(0, "Run checks") { runChecks() }
                action(1, "Inspect memory") { selected = Section.MEMORY; rebuildGui(); refreshView() }
                action(2, "Start 30s FPS test") { setStatus(FpsTestService.start(30).message); refreshView() }
                action(3, "Export report") { exportReport() }
                action(4, "Latest report") { locateLatestReport() }
                action(5, "Refresh diagnostics") { refreshView() }
            }
            Section.PERFORMANCE -> {
                action(0, "Start 30s FPS test") { setStatus(FpsTestService.start(30).message); refreshView() }
                action(1, "Stop FPS test") { setStatus(FpsTestService.stop().message); refreshView() }
                PerformanceMode.entries.forEachIndexed { index, mode ->
                    action(index + 2, mode.label) {
                        val result = PerformanceService.setMode(mode)
                        setStatus(result.message, if (result.success) GOOD else WARN)
                        refreshView()
                    }
                }
            }
            Section.SETTINGS -> {
                action(0, if (ForgeGradleMCConfig.overlayEnabled.get()) "Disable overlay" else "Enable overlay") {
                    val value = !ForgeGradleMCConfig.overlayEnabled.get()
                    setStatus(if (ForgeGradleMCConfig.setOverlayEnabled(value)) "Overlay setting saved." else "Unable to save overlay setting.", if (ForgeGradleMCConfig.overlayEnabled.get() == value) GOOD else WARN)
                    GradleMcStatsOverlay.onSettingsChanged(); refreshView()
                }
                action(1, if (ForgeGradleMCConfig.overlayShowFps.get()) "Hide FPS" else "Show FPS") {
                    val value = !ForgeGradleMCConfig.overlayShowFps.get()
                    setStatus(if (ForgeGradleMCConfig.setOverlayShowFps(value)) "FPS visibility saved." else "Unable to save FPS visibility.", if (ForgeGradleMCConfig.overlayShowFps.get() == value) GOOD else WARN)
                    GradleMcStatsOverlay.onSettingsChanged(); refreshView()
                }
                action(2, if (ForgeGradleMCConfig.overlayShowAverageFps.get()) "Hide average FPS" else "Show average FPS") {
                    val value = !ForgeGradleMCConfig.overlayShowAverageFps.get()
                    setStatus(if (ForgeGradleMCConfig.setOverlayShowAverageFps(value)) "Average FPS visibility saved." else "Unable to save average FPS visibility.", if (ForgeGradleMCConfig.overlayShowAverageFps.get() == value) GOOD else WARN)
                    GradleMcStatsOverlay.onSettingsChanged(); refreshView()
                }
            }
            else -> Unit
        }
    }

    private fun refreshView() {
        lines = when (selected) {
            Section.OVERVIEW -> overviewLines()
            Section.QUICK_ACTIONS -> listOf("Use the actions below to collect diagnostics or write a local report.")
            Section.ENVIRONMENT -> environmentLines()
            Section.MEMORY -> memoryLines()
            Section.PERFORMANCE -> performanceLines()
            Section.STABILITY -> DiagnosticsService.snapshot().lastChecks?.let { checks ->
                listOf("Last checks: ${checks.summary}") + checks.findings.map { "[${it.severity}] ${it.title}: ${it.detail}" }
            } ?: listOf("No checks have been run yet. Use Quick Actions > Run checks.")
            Section.REPORTS -> DiagnosticsService.snapshot().latestReport?.let { listOf("Latest local report:", DiagnosticsService.displayPath(it)) }
                ?: listOf("No GradleMC reports exist yet. Use Quick Actions > Export report.")
            Section.SETTINGS -> settingsLines()
        }
        refreshCountdown = 20
    }

    private fun wrappedLines(layout: GradleMcGuiLayout) = lines.flatMap { line ->
        font.split(Component.literal(line), layout.contentWidth.coerceAtLeast(1))
    }

    private fun overviewLines(): List<String> {
        val snapshot = DiagnosticsService.snapshot()
        val performance = snapshot.performance
        val memory = snapshot.memory
        return listOf(
            "GradleMC ${snapshot.environment.gradleMcVersion}",
            "Rendered frames: ${performance.observedFrames}",
            "FPS: ${fpsLabel(performance.currentFps)} (average ${fpsLabel(performance.averageFps)})",
            "Memory: ${memory.usedMiB}/${memory.maxMiB} MiB (${"%.1f".format(Locale.ROOT, memory.usedPercent)}%)",
            "Performance mode: ${performance.mode.label}",
            "Latest report: ${snapshot.latestReport?.let(DiagnosticsService::displayPath) ?: "none"}",
        )
    }

    private fun environmentLines(): List<String> = DiagnosticsService.snapshot().environment.let { environment -> listOf(
        "Minecraft: ${environment.minecraftVersion}", "Forge: ${environment.forgeVersion}", "Java: ${environment.javaVersion}",
        "OS: ${environment.operatingSystem}", "Architecture: ${environment.architecture}",
        "Physical side: ${environment.physicalSide}", "Installed mods: ${environment.installedModCount ?: "unavailable"}",
    ) }

    private fun memoryLines(): List<String> = DiagnosticsService.snapshot().memory.let { memory -> listOf(
        "Used heap: ${memory.usedMiB} MiB (${String.format(Locale.ROOT, "%.1f", memory.usedPercent)}%)", "Committed heap: ${memory.committedMiB} MiB",
        "Maximum heap: ${memory.maxMiB} MiB", "Available headroom: ${memory.freeMiB} MiB", "Pressure: ${memory.pressure.name.lowercase()}",
    ) }

    private fun performanceLines(): List<String> {
        val snapshot = PerformanceService.snapshot()
        val fps = FpsTestService.state()
        return listOf(snapshot.message, "Current FPS: ${fpsLabel(snapshot.currentFps)}", "Average FPS: ${fpsLabel(snapshot.averageFps)}", "Mode: ${snapshot.mode.label}", if (fps.isRunning) "FPS test: ${"%.1f".format(Locale.ROOT, fps.elapsedSeconds)}/${fps.requestedSeconds}s" else "FPS test: idle")
    }

    private fun settingsLines(): List<String> = listOf(
        "Overlay: ${if (ForgeGradleMCConfig.overlayEnabled.get()) "enabled" else "disabled"}",
        "Show FPS: ${ForgeGradleMCConfig.overlayShowFps.get()}",
        "Show average FPS: ${ForgeGradleMCConfig.overlayShowAverageFps.get()}",
        "GUI keybind enabled: ${ForgeGradleMCConfig.guiKeybindEnabled.get()}",
        "Default key: G (change it in Minecraft Controls)",
    )

    private fun runChecks() {
        val checks = DiagnosticsService.runChecks()
        setStatus("Checks complete: ${checks.summary}.", if (checks.highestSeverity.ordinal >= 2) WARN else GOOD)
        refreshView()
    }

    private fun exportReport() {
        if (exportInProgress) {
            setStatus("A report export is already running.", WARN)
            return
        }
        exportInProgress = true
        setStatus("Exporting report...")
        val client = minecraft ?: run {
            exportInProgress = false
            setStatus("Report export requires an active client.", WARN)
            return
        }
        CompletableFuture.supplyAsync(DiagnosticsService::exportReport, Util.ioPool()).whenComplete { result, failure ->
            client.execute {
                exportInProgress = false
                when {
                    failure != null -> setStatus("Report export failed: ${failure.javaClass.simpleName}", WARN)
                    result is ReportExportResult.Success -> setStatus("Report exported: ${result.displayPath}")
                    result is ReportExportResult.Failure -> setStatus(result.message, WARN)
                }
                refreshView()
            }
        }
    }

    private fun locateLatestReport() {
        when (val latest = DiagnosticsService.latestReportResult()) {
            is com.soumyajit.gradlemc.diagnostics.LatestReportResult.Found -> setStatus("Latest report: ${latest.displayPath}")
            com.soumyajit.gradlemc.diagnostics.LatestReportResult.Empty -> setStatus("No GradleMC reports found yet.", WARN)
            is com.soumyajit.gradlemc.diagnostics.LatestReportResult.Failure -> setStatus(latest.message, WARN)
        }
        refreshView()
    }
    private fun setStatus(message: String, color: Int = GOOD) { status = message; statusColor = color }
    private fun fpsLabel(value: Double?): String = if (value == null) "warming up" else "%.0f".format(Locale.ROOT, value.coerceAtLeast(0.0))
    private fun summaryLineCount(section: Section): Int = when (section) {
        Section.QUICK_ACTIONS -> 1
        Section.PERFORMANCE, Section.SETTINGS -> 5
        else -> 0
    }
    private fun sidebarWidth(layout: GradleMcGuiLayout): Int = (layout.contentLeft - layout.left - 14).coerceAtLeast(54)
    private fun drawBorder(graphics: GuiGraphics, layout: GradleMcGuiLayout) {
        graphics.fill(layout.left, layout.top, layout.right, layout.top + 1, BORDER)
        graphics.fill(layout.left, layout.bottom - 1, layout.right, layout.bottom, BORDER)
        graphics.fill(layout.left, layout.top, layout.left + 1, layout.bottom, BORDER)
        graphics.fill(layout.right - 1, layout.top, layout.right, layout.bottom, BORDER)
    }

    private companion object {
        const val BACKGROUND = 0xE010141C.toInt()
        const val PANEL = 0xD0182230.toInt()
        const val PANEL_DARK = 0xE00C1118.toInt()
        const val BORDER = 0x70465D78
        const val TEXT = 0xFFEAF0F7.toInt()
        const val MUTED = 0xFFB8C3D1.toInt()
        const val GOOD = 0xFF77D38A.toInt()
        const val WARN = 0xFFE6C15A.toInt()
    }
}

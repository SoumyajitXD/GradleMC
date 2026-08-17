package com.soumyajit.gradlemc.client

import com.soumyajit.gradlemc.client.overlay.GradleMcStatsOverlay
import com.soumyajit.gradlemc.config.GradleMcConfig
import com.soumyajit.gradlemc.config.GradleMcConfigSnapshot
import com.soumyajit.gradlemc.diagnostics.DiagnosticSeverity
import com.soumyajit.gradlemc.diagnostics.DiagnosticsService
import com.soumyajit.gradlemc.diagnostics.ReportExportResult
import com.soumyajit.gradlemc.performance.FpsTestService
import com.soumyajit.gradlemc.performance.PerformanceMode
import com.soumyajit.gradlemc.performance.PerformanceService
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import java.util.Locale
import java.util.concurrent.CompletableFuture

/**
 * A deliberately compact Minecraft-native diagnostics dashboard. Measurements and persistence remain
 * in their services; this screen owns only navigation, presentation and short-lived action state.
 */
internal class GradleMcDiagnosticsScreen : Screen(Component.literal("GradleMC Diagnostics")) {
    private enum class Section(val label: String) {
        OVERVIEW("Overview"), QUICK_ACTIONS("Actions"), ENVIRONMENT("Environment"), MEMORY("Memory"),
        PERFORMANCE("Performance"), OVERLAY("Overlay"), STABILITY("Stability"), REPORTS("Reports"), SETTINGS("Settings")
    }
    private enum class JobState { IDLE, RUNNING, COMPLETED, FAILED }
    private data class Block(val title: String, val accent: Int, val rows: List<String>)

    private var selected = Section.OVERVIEW
    private var blocks = emptyList<Block>()
    private var status = "Ready — local diagnostics only."
    private var statusColor = GOOD
    private var jobState = JobState.IDLE
    private var refreshCountdown = 0
    private var scroll = 0

    override fun init() { rebuild(); refresh() }

    override fun tick() {
        if (--refreshCountdown <= 0) refresh()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics)
        val layout = GradleMcGuiLayout.calculate(width, height)
        graphics.fill(layout.left, layout.top, layout.right, layout.bottom, BACKGROUND)
        graphics.fill(layout.left, layout.top, layout.right, layout.headerBottom, HEADER)
        if (!layout.compactNavigation) graphics.fill(layout.left, layout.headerBottom, layout.contentLeft - 8, layout.footerTop, NAVIGATION)
        graphics.fill(layout.contentLeft - 7, layout.contentTop - 5, layout.right - 14, layout.footerTop - 6, PANEL)
        graphics.fill(layout.left, layout.footerTop, layout.right, layout.bottom, HEADER)
        graphics.drawString(font, "GradleMC", layout.left + 14, layout.top + 11, TEXT, false)
        graphics.drawString(font, "Diagnostics dashboard · Fabric 1.20.1", layout.left + 14, layout.top + 27, MUTED, false)
        graphics.drawString(font, selected.label, layout.contentLeft, layout.contentTop, TEXT, false)
        renderBlocks(graphics, layout)
        graphics.drawString(font, status, layout.left + 14, layout.footerTop + 11, statusColor, false)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        val layout = GradleMcGuiLayout.calculate(width, height)
        if (mouseX !in layout.contentLeft.toDouble()..(layout.right - 14).toDouble() || mouseY !in layout.contentTop.toDouble()..layout.contentBottom.toDouble()) return super.mouseScrolled(mouseX, mouseY, amount)
        scroll = (scroll + if (amount < 0) 2 else -2).coerceAtLeast(0)
        return true
    }

    private fun renderBlocks(graphics: GuiGraphics, layout: GradleMcGuiLayout) {
        val entries = blocks.flatMap { block -> listOf(BlockLine(Component.literal(block.title).visualOrderText, block.accent, true)) + block.rows.flatMap { row -> font.split(Component.literal(row), layout.contentWidth - 16).map { BlockLine(it, MUTED, false) } } }
        val contentStart = layout.contentTop + 18 + actionRows(layout) * 23 + if (hasActions()) 6 else 0
        val visible = ((layout.contentBottom - contentStart) / (font.lineHeight + 3)).coerceAtLeast(0)
        scroll = scroll.coerceIn(0, (entries.size - visible).coerceAtLeast(0))
        var y = contentStart
        entries.drop(scroll).take(visible).forEach { entry ->
            if (entry.heading) graphics.fill(layout.contentLeft, y - 1, layout.contentLeft + 3, y + font.lineHeight + 1, entry.color)
            graphics.drawString(font, entry.text, layout.contentLeft + if (entry.heading) 8 else 10, y, entry.color, false)
            y += font.lineHeight + 3
        }
    }

    private data class BlockLine(val text: FormattedCharSequence, val color: Int, val heading: Boolean)

    private fun rebuild() {
        clearWidgets()
        val layout = GradleMcGuiLayout.calculate(width, height)
        Section.entries.zip(layout.navigationTabs(Section.entries.size)).forEach { (section, bounds) ->
            addRenderableWidget(Button.builder(Component.literal(section.label)) {
                selected = section; scroll = 0; rebuild(); refresh()
            }.bounds(bounds.x, bounds.y, bounds.width, bounds.height).build().also {
                it.active = section != selected
                it.tooltip = Tooltip.create(Component.literal(if (section == selected) "Current section" else "Open ${section.label}"))
            })
        }
        addRenderableWidget(Button.builder(Component.literal("Refresh")) { refresh(true) }
            .bounds(layout.right - 190, layout.footerTop + 7, 88, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Close")) { onClose() }
            .bounds(layout.right - 96, layout.footerTop + 7, 82, 20).build())
        addSectionActions(layout)
    }

    private fun addSectionActions(layout: GradleMcGuiLayout) {
        val actionY = layout.contentTop + 18
        var index = 0
        fun action(label: String, enabled: Boolean = jobState != JobState.RUNNING, detail: String = label, run: () -> Unit) {
            val columns = actionColumns(layout)
            val buttonWidth = ((layout.contentWidth - 8 * (columns - 1)) / columns).coerceAtLeast(1)
            val x = layout.contentLeft + (index % columns) * (buttonWidth + 8)
            val y = actionY + (index / columns) * 23
            index++
            addRenderableWidget(Button.builder(Component.literal(label)) { run() }.bounds(x, y, buttonWidth, 20).build().also {
                it.active = enabled
                it.tooltip = Tooltip.create(Component.literal(detail))
            })
        }
        fun overlay(label: String, change: (GradleMcConfigSnapshot) -> GradleMcConfigSnapshot) = action(label, detail = "Saved to GradleMC configuration") {
            runCatching { GradleMcConfig.update(change); GradleMcStatsOverlay.invalidate() }
                .onSuccess { notice("Saved: $label") }
                .onFailure { failure("Unable to save setting: ${it.message ?: it.javaClass.simpleName}") }
        }
        when (selected) {
            Section.QUICK_ACTIONS -> {
                action("Run checks", detail = "Evaluate local stability checks") { runAsync("Running local checks…") { DiagnosticsService.runChecks().let { "Checks complete: ${it.summary}." } } }
                action("Inspect memory", detail = "Open live heap details") { selected = Section.MEMORY; rebuild(); refresh() }
                action("Start 30s FPS test", enabled = !FpsTestService.state().isRunning, detail = "Measure rendered frame intervals for 30 seconds") { notice(FpsTestService.start(30).message) }
                action("Stop FPS test", enabled = FpsTestService.state().isRunning, detail = "Stop the currently running FPS test") { notice(FpsTestService.stop().message) }
                action("Export report", detail = "Write truthful local TXT and JSON reports") { runAsync("Exporting local report…") { when (val result = DiagnosticsService.exportReport()) { is ReportExportResult.Success -> "Exported: ${result.displayPaths.joinToString()}."; is ReportExportResult.Failure -> throw IllegalStateException(result.message) } } }
                action("Smart diagnostics", detail = "Show deterministic, local rule-based advice") { runAsync("Calculating Smart Diagnostics…") { val result = DiagnosticsService.smartScore(); "Smart score ${result.score}/100 — ${result.advice.firstOrNull() ?: result.message}" } }
            }
            Section.PERFORMANCE -> {
                action("Start 30s FPS test", enabled = !FpsTestService.state().isRunning) { notice(FpsTestService.start(30).message) }
                action("Stop FPS test", enabled = FpsTestService.state().isRunning) { notice(FpsTestService.stop().message) }
                PerformanceMode.entries.forEach { mode -> action("Mode: ${mode.label}", detail = "${mode.label}: ${modeDescription(mode)}") { notice(PerformanceService.setMode(mode).message) } }
            }
            Section.OVERLAY -> {
                val config = GradleMcConfig.current()
                overlay(if (config.overlayEnabled) "Disable overlay" else "Enable overlay") { it.copy(overlayEnabled = !it.overlayEnabled) }
                overlay(if (config.overlayShowTitle) "Hide overlay title" else "Show overlay title") { it.copy(overlayShowTitle = !it.overlayShowTitle) }
                overlay(if (config.overlayShowFps) "Hide current FPS" else "Show current FPS") { it.copy(overlayShowFps = !it.overlayShowFps) }
                overlay(if (config.overlayShowAverageFps) "Hide average FPS" else "Show average FPS") { it.copy(overlayShowAverageFps = !it.overlayShowAverageFps) }
                listOf(30, 60, 120).forEach { seconds -> overlay("Average window: ${seconds}s") { it.copy(overlaySamplingWindowSeconds = seconds) } }
            }
            Section.REPORTS -> {
                action("Export new report") { runAsync("Exporting local report…") { when (val result = DiagnosticsService.exportReport()) { is ReportExportResult.Success -> "Exported: ${result.displayPaths.joinToString()}."; is ReportExportResult.Failure -> throw IllegalStateException(result.message) } } }
                action("Refresh reports") { refresh(true) }
            }
            Section.SETTINGS -> action("Overlay controls") { selected = Section.OVERLAY; rebuild(); refresh() }
            else -> Unit
        }
    }

    private fun refresh(manual: Boolean = false) {
        val snapshot = DiagnosticsService.snapshot()
        blocks = when (selected) {
            Section.OVERVIEW -> overview(snapshot)
            Section.QUICK_ACTIONS -> listOf(Block("WHAT YOU CAN DO", ACCENT, listOf("Run local checks, inspect heap health, measure rendered frames, export a private local report, or get deterministic Smart Diagnostics advice.", "Actions disable while file work is running, preventing duplicate exports.")))
            Section.ENVIRONMENT -> environment(snapshot)
            Section.MEMORY -> memory(snapshot)
            Section.PERFORMANCE -> performance(snapshot)
            Section.OVERLAY -> overlay(snapshot.configuration)
            Section.STABILITY -> stability(snapshot)
            Section.REPORTS -> reports(snapshot)
            Section.SETTINGS -> settings(snapshot.configuration)
        }
        if (manual && jobState != JobState.RUNNING) notice("Dashboard refreshed.", refresh = false)
        refreshCountdown = 10
    }

    private fun overview(s: com.soumyajit.gradlemc.diagnostics.DiagnosticSnapshot) = listOf(
        Block("RUNTIME", ACCENT, listOf("GradleMC ${s.environment.gradleMcVersion} · Minecraft ${s.environment.minecraftVersion}", "Fabric Loader ${s.environment.loaderVersion} · ${s.environment.installedModCount} installed mods", "Java ${s.environment.javaVersion} · ${s.environment.operatingSystem}")),
        Block("LIVE PERFORMANCE", GOOD, listOf("Current FPS: ${fps(s.performance.currentFps)}", "Rolling average: ${fps(s.performance.averageFps)} · ${s.performance.observedFrames} observed intervals", "Mode: ${s.performance.mode.label} — ${modeDescription(s.performance.mode)}")),
        Block("HEALTH", severityColor(s.memory.pressure), listOf("Heap: ${s.memory.usedMiB} / ${s.memory.maxMiB} MiB (${percent(s.memory.usedPercent)})", "Diagnostics: ${s.lastChecks?.summary ?: "not run yet"}", "Latest report: ${s.latestReport?.let(DiagnosticsService::displayPath) ?: "none yet"}")),
    )

    private fun environment(s: com.soumyajit.gradlemc.diagnostics.DiagnosticSnapshot) = listOf(
        Block("MINECRAFT & FABRIC", ACCENT, listOf("Minecraft: ${s.environment.minecraftVersion}", "Fabric Loader: ${s.environment.loaderVersion}", "GradleMC: ${s.environment.gradleMcVersion}", "Environment: ${s.environment.physicalSide} · ${s.environment.installedModCount} installed mods")),
        Block("JAVA & SYSTEM", GOOD, listOf("Runtime: ${s.environment.javaVersion}", "Vendor: ${s.environment.javaVendor}", "Operating system: ${s.environment.operatingSystem}", "Architecture: ${s.environment.architecture}")),
    )

    private fun memory(s: com.soumyajit.gradlemc.diagnostics.DiagnosticSnapshot) = listOf(
        Block("HEAP SNAPSHOT", severityColor(s.memory.pressure), listOf("Used: ${s.memory.usedMiB} MiB (${percent(s.memory.usedPercent)})", "Committed: ${s.memory.committedMiB} MiB", "Maximum heap: ${s.memory.maxMiB} MiB", "Available headroom: ${s.memory.freeMiB} MiB")),
        Block("INTERPRETATION", ACCENT, listOf("Pressure: ${s.memory.pressure.name.lowercase(Locale.ROOT)}", memoryAdvice(s.memory.pressure), "GradleMC reports observed heap state; it never claims to optimise RAM or invokes garbage collection.")),
    )

    private fun performance(s: com.soumyajit.gradlemc.diagnostics.DiagnosticSnapshot): List<Block> {
        val test = FpsTestService.state()
        val testLine = if (test.isRunning) "Running: ${"%.1f".format(Locale.ROOT, test.elapsedSeconds)} / ${test.requestedSeconds}s" else test.latestResult?.let { "Last test: ${it.endReason.name.lowercase(Locale.ROOT)} · ${"%.1f".format(Locale.ROOT, it.averageFps)} average FPS · ${it.sampleCount} intervals" } ?: "No explicit FPS test has run."
        return listOf(
            Block("RENDERED-FRAME TIMING", GOOD, listOf("Current FPS: ${fps(s.performance.currentFps)}", "Rolling average: ${fps(s.performance.averageFps)}", "Evidence: ${s.performance.message}")),
            Block("FPS TEST", ACCENT, listOf(testLine, "Tests use completed render-frame intervals, not tick-rate estimates.")),
            Block("SAMPLING MODE", ACCENT, listOf("${s.performance.mode.label}: ${modeDescription(s.performance.mode)}", "${PerformanceService.guardDescription()}")),
        )
    }

    private fun overlay(c: GradleMcConfigSnapshot) = listOf(
        Block("MASTER STATE", if (c.overlayEnabled) GOOD else MUTED, listOf(if (c.overlayEnabled) "Enabled — visible during gameplay only." else "Disabled — subordinate display preferences are retained but not rendered.")),
        Block("DISPLAY PREFERENCES", ACCENT, listOf("Title: ${state(c.overlayShowTitle)}", "Current FPS: ${state(c.overlayShowFps)}", "Average FPS: ${state(c.overlayShowAverageFps)}", "Averaging window: ${c.overlaySamplingWindowSeconds} seconds")),
        Block("OVERHEAD", GOOD, listOf("The overlay is hidden in menus and with F3 debug information.", "Text/layout is cached; it does not run diagnostics each frame.")),
    )

    private fun stability(s: com.soumyajit.gradlemc.diagnostics.DiagnosticSnapshot): List<Block> {
        val checks = s.lastChecks ?: return listOf(Block("CHECK STATUS", ACCENT, listOf("Checks have not been run in this session.", "Use Actions → Run checks for local, evidence-based findings.")))
        return listOf(Block("LAST LOCAL CHECKS", severityColor(checks.highestSeverity), listOf("${checks.summary} · ${checks.ranAt}") + checks.findings.flatMap { finding -> listOf("[${finding.severity}] ${finding.title}: ${finding.detail}", finding.suggestion.takeIf(String::isNotBlank)?.let { "Recommendation: $it" } ?: "") }.filter(String::isNotBlank)))
    }

    private fun reports(s: com.soumyajit.gradlemc.diagnostics.DiagnosticSnapshot) = listOf(
        Block("LOCAL REPORTS", ACCENT, listOf(s.latestReport?.let { "Latest: ${DiagnosticsService.displayPath(it)}" } ?: "No GradleMC report exists yet.", "Export writes a truthful TXT and JSON pair under the local game directory.", "GradleMC has no telemetry, cloud upload, or hidden network requests.")),
    )

    private fun settings(c: GradleMcConfigSnapshot) = listOf(
        Block("CONTROLS", ACCENT, listOf("Open dashboard key: G", "Change the key in Minecraft's Controls menu; GradleMC does not duplicate Minecraft's keybinding UI.", "Keybind enabled: ${state(c.keyBindingEnabled)}")),
        Block("DIAGNOSTICS", GOOD, listOf("Performance mode: ${PerformanceMode.parse(c.performanceMode).label}", "Overlay average window: ${c.overlaySamplingWindowSeconds} seconds", "Report directory: ${c.reportDirectoryName}")),
    )

    private fun runAsync(working: String, task: () -> String) {
        if (jobState == JobState.RUNNING) return
        jobState = JobState.RUNNING
        status = working
        statusColor = ACCENT
        rebuild()
        CompletableFuture.supplyAsync(task).whenComplete { message, throwable ->
            minecraft?.execute {
                if (throwable == null) {
                    jobState = JobState.COMPLETED
                    notice(message)
                } else {
                    jobState = JobState.FAILED
                    failure(throwable.cause?.message ?: throwable.message ?: throwable.javaClass.simpleName)
                }
                rebuild()
            }
        }
    }

    private fun notice(message: String, refresh: Boolean = true) { status = message; statusColor = GOOD; if (refresh) refresh() }
    private fun failure(message: String) { status = message; statusColor = BAD; refresh() }
    private fun fps(value: Double?) = value?.takeIf(Double::isFinite)?.coerceAtLeast(0.0)?.let { "%.0f".format(Locale.ROOT, it) } ?: "warming up"
    private fun percent(value: Double) = "%.1f%%".format(Locale.ROOT, value)
    private fun state(value: Boolean) = if (value) "shown" else "hidden"
    private fun memoryAdvice(severity: DiagnosticSeverity) = when (severity) { DiagnosticSeverity.CRITICAL -> "Heap pressure is critically high; reduce competing memory use and investigate sustained pressure."; DiagnosticSeverity.WARN -> "Heap pressure is elevated; investigate only if it persists during normal play."; else -> "No shallow heap-pressure warning is currently present." }
    private fun modeDescription(mode: PerformanceMode) = when (mode) { PerformanceMode.LOW_IMPACT -> "groups four frame intervals for the lowest collection overhead"; PerformanceMode.BALANCED -> "groups two frame intervals for normal diagnostic use"; PerformanceMode.DETAILED -> "keeps each frame interval for maximum timing detail" }
    private fun severityColor(severity: DiagnosticSeverity) = when (severity) { DiagnosticSeverity.CRITICAL, DiagnosticSeverity.FAIL -> BAD; DiagnosticSeverity.WARN -> WARN; DiagnosticSeverity.PASS -> GOOD; DiagnosticSeverity.INFO -> ACCENT }
    private fun actionColumns(layout: GradleMcGuiLayout) = if (layout.contentWidth >= 370 || (layout.compactNavigation && layout.contentWidth >= 250)) 2 else 1
    private fun actionRows(layout: GradleMcGuiLayout): Int {
        val actions = when (selected) {
            Section.QUICK_ACTIONS -> 6; Section.PERFORMANCE -> 5; Section.OVERLAY -> 7; Section.REPORTS -> 2; Section.SETTINGS -> 1; else -> 0
        }
        return (actions + actionColumns(layout) - 1) / actionColumns(layout)
    }
    private fun hasActions() = selected in setOf(Section.QUICK_ACTIONS, Section.PERFORMANCE, Section.OVERLAY, Section.REPORTS, Section.SETTINGS)

    companion object {
        private const val BACKGROUND = 0xE00E131B.toInt(); private const val PANEL = 0xD0182633.toInt(); private const val HEADER = 0xE0090E15.toInt(); private const val NAVIGATION = 0xE0111923.toInt()
        private const val TEXT = 0xFFF0F5FA.toInt(); private const val MUTED = 0xFFB7C5D4.toInt(); private const val GOOD = 0xFF78D692.toInt(); private const val WARN = 0xFFF2C15D.toInt(); private const val BAD = 0xFFFF7A7A.toInt(); private const val ACCENT = 0xFF7CB8FF.toInt()
    }
}

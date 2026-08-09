package com.soumyajit.gradlemc.diagnostics

import com.soumyajit.gradlemc.GradleMC
import com.soumyajit.gradlemc.config.ForgeGradleMCConfig
import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import com.soumyajit.gradlemc.performance.PerformanceService
import com.soumyajit.gradlemc.performance.PerformanceSnapshot
import com.soumyajit.gradlemc.report.LatestReportFinder
import com.soumyajit.gradlemc.report.LatestReportLookup
import com.soumyajit.gradlemc.report.ReportDirectoryProbe
import com.soumyajit.gradlemc.report.ReportDirectoryReadiness
import com.soumyajit.gradlemc.report.ReportFilePublisher
import com.soumyajit.gradlemc.report.ReportNameGenerator
import com.soumyajit.gradlemc.report.ReportPathDisplay
import com.soumyajit.gradlemc.report.ReportPaths
import net.minecraft.SharedConstants
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.versions.forge.ForgeVersion
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import java.time.Clock
import java.time.ZoneId

enum class DiagnosticSeverity { PASS, INFO, WARN, FAIL, CRITICAL }

data class EnvironmentSnapshot(
    val gradleMcVersion: String,
    val minecraftVersion: String,
    val forgeVersion: String,
    val javaVersion: String,
    val operatingSystem: String,
    val architecture: String,
    val physicalSide: String,
    val installedModCount: Int?,
)

data class MemorySnapshot(
    val usedBytes: Long,
    val committedBytes: Long,
    val maxBytes: Long,
    val freeHeadroomBytes: Long,
    val usedPercent: Double,
    val pressure: DiagnosticSeverity,
    val collectedAt: Instant,
) {
    val usedMiB: Long get() = usedBytes / MEBIBYTE
    val committedMiB: Long get() = committedBytes / MEBIBYTE
    val maxMiB: Long get() = maxBytes / MEBIBYTE
    val freeMiB: Long get() = freeHeadroomBytes / MEBIBYTE
    companion object { const val MEBIBYTE = 1024L * 1024L }
}

data class StabilityFinding(val severity: DiagnosticSeverity, val title: String, val detail: String, val suggestion: String = "")
data class StabilityCheckResult(val ranAt: Instant, val findings: List<StabilityFinding>) {
    val highestSeverity: DiagnosticSeverity get() = findings.maxByOrNull { it.severity.ordinal }?.severity ?: DiagnosticSeverity.INFO
    val summary: String get() = findings.groupingBy { it.severity }.eachCount().entries.sortedBy { it.key.ordinal }
        .joinToString(", ") { "${it.value} ${it.key.name.lowercase()}" }
}

data class DiagnosticSnapshot(
    val collectedAt: Instant,
    val environment: EnvironmentSnapshot,
    val memory: MemorySnapshot,
    val performance: PerformanceSnapshot,
    val configuration: GradleMCConfigSnapshot,
    val latestReport: Path?,
    val lastChecks: StabilityCheckResult?,
)

sealed interface ReportExportResult {
    data class Success(val path: Path, val snapshot: DiagnosticSnapshot, val displayPath: String) : ReportExportResult
    data class Failure(val message: String, val cause: Throwable? = null) : ReportExportResult
}

sealed interface LatestReportResult {
    data class Found(val path: Path, val displayPath: String) : LatestReportResult
    data object Empty : LatestReportResult
    data class Failure(val message: String, val cause: Throwable? = null) : LatestReportResult
}

data class SmartDiagnosticsResult(val score: Int, val advice: List<String>, val message: String, val basedOn: StabilityCheckResult)

/** Common-side diagnostics facade. Call it from commands or explicit GUI refreshes, never each render frame. */
object DiagnosticsService {
    @Volatile private var lastChecks: StabilityCheckResult? = null

    fun snapshot(): DiagnosticSnapshot {
        val config = ForgeGradleMCConfig.snapshot()
        return DiagnosticSnapshot(Instant.now(), environment(), memory(), PerformanceService.snapshot(), config, latestReport(config), lastChecks)
    }

    fun runChecks(): StabilityCheckResult {
        val config = ForgeGradleMCConfig.snapshot()
        val memory = memory()
        val findings = buildList {
            add(StabilityFinding(memory.pressure, "Runtime memory snapshot",
                "Used ${memory.usedMiB} MiB, committed ${memory.committedMiB} MiB, max ${memory.maxMiB} MiB.",
                if (memory.pressure.ordinal >= DiagnosticSeverity.WARN.ordinal) "Close memory-heavy applications or allocate more heap if this remains high." else ""))
            add(StabilityFinding(DiagnosticSeverity.PASS, "GradleMC configuration is valid",
                "Reports use the '${config.reportDirectoryName}' directory and ${config.performanceMode} diagnostic mode."))
            val gameDirectory = gameDirectory()
            add(when (val readiness = ReportDirectoryProbe.inspect(gameDirectory, config)) {
                is ReportDirectoryReadiness.Ready -> StabilityFinding(
                    if (readiness.alreadyExists) DiagnosticSeverity.PASS else DiagnosticSeverity.INFO,
                    if (readiness.alreadyExists) "Report directory is writable" else "Report directory will be created on export",
                    ReportPathDisplay.relativeToGame(gameDirectory, readiness.directory),
                )
                is ReportDirectoryReadiness.Failure -> StabilityFinding(
                    DiagnosticSeverity.FAIL,
                    "Report output is unavailable",
                    "${readiness.message}: ${ReportPathDisplay.relativeToGame(gameDirectory, readiness.directory)}",
                    "Check the GradleMC report directory permissions and remove unsafe filesystem links.",
                )
            })
            val javaFeature = Runtime.version().feature()
            add(StabilityFinding(if (javaFeature >= 17) DiagnosticSeverity.PASS else DiagnosticSeverity.FAIL, "Java runtime compatibility",
                "Running Java $javaFeature.", if (javaFeature >= 17) "" else "Minecraft Forge 1.20.1 requires Java 17 or newer."))
        }
        return StabilityCheckResult(Instant.now(), findings).also { lastChecks = it }
    }

    fun exportReport(): ReportExportResult {
        val checks = runChecks()
        val snapshot = snapshot()
        return try {
            val gameDirectory = gameDirectory()
            val path = ReportFilePublisher(ReportNameGenerator(Clock.systemUTC(), ZoneId.systemDefault()))
                .publish(gameDirectory, snapshot.configuration, DiagnosticsReportRenderer.render(snapshot, checks, gameDirectory))
            ReportExportResult.Success(path, snapshot.copy(latestReport = path), ReportPathDisplay.relativeToGame(gameDirectory, path))
        } catch (exception: IOException) {
            ReportExportResult.Failure("Report export failed: ${safeMessage(exception)}", exception)
        } catch (exception: RuntimeException) {
            ReportExportResult.Failure("Report export failed: ${safeMessage(exception)}", exception)
        }
    }

    fun latestReport(): Path? = latestReport(ForgeGradleMCConfig.snapshot())

    fun latestReportResult(): LatestReportResult = latestReportResult(ForgeGradleMCConfig.snapshot())

    fun displayPath(path: Path): String = ReportPathDisplay.relativeToGame(gameDirectory(), path)

    fun smartScore(): SmartDiagnosticsResult = smart(runChecks())
    fun smartAdvice(): SmartDiagnosticsResult = smart(runChecks())

    private fun smart(checks: StabilityCheckResult): SmartDiagnosticsResult {
        val deduction = checks.findings.sumOf {
            when (it.severity) { DiagnosticSeverity.CRITICAL -> 45; DiagnosticSeverity.FAIL -> 25; DiagnosticSeverity.WARN -> 10; else -> 0 }
        }
        val advice = checks.findings.filter { it.severity.ordinal >= DiagnosticSeverity.WARN.ordinal }
            .map { it.suggestion.ifBlank { it.detail } }
            .ifEmpty { listOf("No shallow stability warnings are currently present.") }
        return SmartDiagnosticsResult((100 - deduction).coerceIn(0, 100), advice,
            "This is a local score based only on GradleMC's currently implemented shallow checks; adaptive donor diagnostics are not yet ported.", checks)
    }

    private fun environment(): EnvironmentSnapshot = EnvironmentSnapshot(
        gradleMcVersion = GradleMC.VERSION,
        minecraftVersion = runCatching { SharedConstants.getCurrentVersion().name }.getOrDefault(GradleMC.MINECRAFT_VERSION),
        forgeVersion = runCatching { ForgeVersion.getVersion() }.getOrDefault(GradleMC.FORGE_VERSION),
        javaVersion = System.getProperty("java.version", "unknown"),
        operatingSystem = OperatingSystemInfoProvider.current().displayName,
        architecture = System.getProperty("os.arch", "unknown"),
        physicalSide = FMLEnvironment.dist.name,
        installedModCount = runCatching { ModList.get().mods.size }.getOrNull(),
    )

    private fun memory(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val committed = runtime.totalMemory()
        val used = committed - runtime.freeMemory()
        val max = runtime.maxMemory()
        val percentage = if (max <= 0L) 0.0 else used * 100.0 / max
        val severity = when { percentage >= 95.0 -> DiagnosticSeverity.CRITICAL; percentage >= 80.0 -> DiagnosticSeverity.WARN; else -> DiagnosticSeverity.PASS }
        return MemorySnapshot(used, committed, max, (max - used).coerceAtLeast(0L), percentage, severity, Instant.now())
    }

    private fun latestReport(config: GradleMCConfigSnapshot): Path? {
        return when (val result = latestReportResult(config)) {
            is LatestReportResult.Found -> result.path
            LatestReportResult.Empty, is LatestReportResult.Failure -> null
        }
    }

    private fun latestReportResult(config: GradleMCConfigSnapshot): LatestReportResult {
        val gameDirectory = gameDirectory()
        return when (val lookup = LatestReportFinder.find(ReportPaths.reportDirectory(gameDirectory, config))) {
            is LatestReportLookup.Found -> LatestReportResult.Found(
                lookup.path,
                ReportPathDisplay.relativeToGame(gameDirectory, lookup.path),
            )
            LatestReportLookup.Empty -> LatestReportResult.Empty
            is LatestReportLookup.Failure -> LatestReportResult.Failure(
                "Latest report lookup failed: ${lookup.message}",
                lookup.cause,
            )
        }
    }

    private fun gameDirectory(): Path = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize()
    private fun safeMessage(error: Throwable): String {
        val raw = error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
        return ReportPathDisplay.redact(raw, gameDirectory())
    }

}

/** Pure text boundary kept separate so report content and privacy can be verified without a running game. */
internal object DiagnosticsReportRenderer {
    fun render(snapshot: DiagnosticSnapshot, checks: StabilityCheckResult, gameDirectory: Path): String = buildString {
        appendLine("GradleMC Diagnostics Report")
        appendLine("Generated: ${snapshot.collectedAt}")
        appendLine("Report directory: ${ReportPathDisplay.relativeToGame(gameDirectory, ReportPaths.reportDirectory(gameDirectory, snapshot.configuration))}")
        appendLine()
        appendLine("Environment")
        appendLine("GradleMC: ${snapshot.environment.gradleMcVersion}")
        appendLine("Minecraft: ${snapshot.environment.minecraftVersion}")
        appendLine("Forge: ${snapshot.environment.forgeVersion}")
        appendLine("Java: ${snapshot.environment.javaVersion}")
        appendLine("Operating system: ${snapshot.environment.operatingSystem}")
        appendLine("Architecture: ${snapshot.environment.architecture}")
        appendLine("Physical side: ${snapshot.environment.physicalSide}")
        appendLine("Installed mods: ${snapshot.environment.installedModCount ?: "unavailable"}")
        appendLine()
        appendLine("Memory")
        appendLine("Used: ${snapshot.memory.usedMiB} MiB / ${snapshot.memory.maxMiB} MiB (${String.format(java.util.Locale.ROOT, "%.1f", snapshot.memory.usedPercent)}%)")
        appendLine("Performance mode: ${snapshot.performance.mode.label}")
        appendLine("Rendered FPS: ${snapshot.performance.currentFps?.let { String.format(java.util.Locale.ROOT, "%.1f", it) } ?: "not collected"}")
        appendLine("Average rendered FPS: ${snapshot.performance.averageFps?.let { String.format(java.util.Locale.ROOT, "%.1f", it) } ?: "not collected"}")
        appendLine()
        appendLine("Stability checks (${checks.summary})")
        checks.findings.forEach { finding ->
            appendLine("[${finding.severity}] ${finding.title}: ${ReportPathDisplay.redact(finding.detail, gameDirectory)}")
        }
    }
}

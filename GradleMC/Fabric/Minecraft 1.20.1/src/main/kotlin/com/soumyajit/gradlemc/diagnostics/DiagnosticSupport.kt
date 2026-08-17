package com.soumyajit.gradlemc.diagnostics

import com.soumyajit.gradlemc.config.GradleMcConfig
import com.soumyajit.gradlemc.config.GradleMcConfigSnapshot
import com.soumyajit.gradlemc.report.IssueBundleWriter
import com.soumyajit.gradlemc.report.ReportFiles
import com.soumyajit.gradlemc.report.ReportPaths
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

data class GradleMcFileLocations(val outputRoot: Path, val reports: Path, val configuration: Path, val latestReport: Path?)
data class ConfigCheckResult(val valid: Boolean, val messages: List<String>)
sealed interface IssueBundleResult {
    data class Success(val path: Path, val displayPath: String) : IssueBundleResult
    data class Failure(val message: String, val cause: Throwable? = null) : IssueBundleResult
}

/** Small command/GUI-facing helpers. All returned locations remain GradleMC-owned. */
object DiagnosticSupport {
    fun locations(config: GradleMcConfigSnapshot = GradleMcConfig.current(), gameDirectory: Path = FabricLoader.getInstance().gameDir): GradleMcFileLocations {
        val root = gameDirectory.toAbsolutePath().normalize().resolve("gradlemc").normalize()
        require(root.startsWith(gameDirectory.toAbsolutePath().normalize()))
        val reports = ReportPaths.reportDirectory(gameDirectory, config)
        return GradleMcFileLocations(root, reports, root.resolve("gradlemc.properties"), ReportFiles.latest(reports))
    }

    fun reports(limit: Int = 12): List<Path> = ReportFiles.list(locations().reports, limit)

    fun checkConfig(config: GradleMcConfigSnapshot = GradleMcConfig.current()): ConfigCheckResult {
        val messages = buildList {
            if (GradleMcConfigSnapshot.safeDirectory(config.reportDirectoryName) == null) add("Report directory name is unsafe.")
            if (config.overlaySamplingWindowSeconds !in setOf(30, 60, 120)) add("Overlay sampling window must be 30, 60, or 120 seconds.")
            if (config.performanceMode !in setOf("low_impact", "balanced", "detailed")) add("Performance mode is unsupported.")
        }
        return ConfigCheckResult(messages.isEmpty(), messages.ifEmpty { listOf("GradleMC configuration is valid.") })
    }

    /** Creates the existing two-entry, allowlisted bundle; it performs no upload or directory traversal. */
    fun createIssueBundle(): IssueBundleResult = try {
        val checks = DiagnosticsService.runChecks()
        val snapshot = DiagnosticsService.snapshot()
        val locations = locations(snapshot.configuration)
        val path = IssueBundleWriter.write(locations.outputRoot, DiagnosticsReportRenderer.text(snapshot, checks), DiagnosticsReportRenderer.json(snapshot, checks))
        IssueBundleResult.Success(path, DiagnosticsService.displayPath(path))
    } catch (e: Exception) {
        IssueBundleResult.Failure("Issue bundle creation failed: ${e.message ?: e.javaClass.simpleName}", e)
    }
}

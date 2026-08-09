package com.soumyajit.gradlemc.diagnostics

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import com.soumyajit.gradlemc.performance.PerformanceMode
import com.soumyajit.gradlemc.performance.PerformanceSnapshot
import com.soumyajit.gradlemc.report.ReportFilePublisher
import com.soumyajit.gradlemc.report.ReportNameGenerator
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticsReportRendererTest {
    @Test
    fun `representative published report is readable and excludes private roots and secret sources`() {
        val evidenceRoot = Path.of(System.getProperty("user.dir"), "build", "tmp")
        Files.createDirectories(evidenceRoot)
        val game = Files.createTempDirectory(evidenceRoot, "diagnostics-report-evidence-").toAbsolutePath().normalize()
        val generatedAt = Instant.parse("2026-08-02T01:02:03Z")
        val config = GradleMCConfigSnapshot.defaults()
        val checks = StabilityCheckResult(generatedAt, listOf(
            StabilityFinding(
                DiagnosticSeverity.PASS,
                "Report directory is writable",
                "Verified ${game.resolve("gradlemc").resolve("reports")}",
            ),
        ))
        val snapshot = DiagnosticSnapshot(
            generatedAt,
            EnvironmentSnapshot("1.1.0", "1.20.1", "47.4.22", "17.0.17", "Windows test fixture", "amd64", "DEDICATED_SERVER", 3),
            MemorySnapshot(128L shl 20, 256L shl 20, 512L shl 20, 384L shl 20, 25.0, DiagnosticSeverity.PASS, generatedAt),
            PerformanceSnapshot(PerformanceMode.BALANCED, null, null, 0, "No samples.", 5, 2),
            config,
            null,
            checks,
        )

        val content = DiagnosticsReportRenderer.render(snapshot, checks, game)
        val path = ReportFilePublisher(ReportNameGenerator(Clock.fixed(generatedAt, ZoneOffset.UTC), ZoneOffset.UTC))
            .publish(game, config, content)
        val published = path.readText()

        assertTrue(published.startsWith("GradleMC Diagnostics Report\nGenerated: 2026-08-02T01:02:03Z"))
        assertTrue(published.contains("Report directory: gradlemc"))
        assertTrue(published.contains("[PASS] Report directory is writable: Verified [game-dir]"))
        assertFalse(published.contains(game.toString(), ignoreCase = true))
        val home = System.getProperty("user.home")
        assertFalse(home.isNullOrBlank())
        assertFalse(published.contains(home, ignoreCase = true))
        listOf("USERPROFILE=", "JAVA_HOME=", "PATH=", "authorization", "bearer ", "api_key", "password", "token=")
            .forEach { marker -> assertFalse(published.contains(marker, ignoreCase = true), "Unexpected private marker: $marker") }
    }
}

package com.soumyajit.gradlemc.diagnostics

import com.soumyajit.gradlemc.performance.PerformanceMode
import com.soumyajit.gradlemc.performance.PerformanceSnapshot
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

class DiagnosticsReportRendererTest {
    @Test fun `JSON report includes Java runtime identity`() {
        val environment = EnvironmentSnapshot("1.1.0", "1.20.1", "0.19.3", "17.0.17", "Temurin", "Windows", "amd64", "CLIENT", 1, listOf("gradlemc:1.1.0"))
        val memory = MemorySnapshot(1, 2, 4, 3, 25.0, DiagnosticSeverity.PASS, Instant.EPOCH)
        val snapshot = DiagnosticSnapshot(Instant.EPOCH, environment, memory, PerformanceSnapshot(PerformanceMode.BALANCED, null, null, 0, "none", 60, 2), com.soumyajit.gradlemc.config.GradleMcConfigSnapshot(), null, null)
        val json = DiagnosticsReportRenderer.json(snapshot, StabilityCheckResult(Instant.EPOCH, emptyList()))
        assertTrue(json.contains("\"javaVersion\":\"17.0.17\"")); assertTrue(json.contains("\"javaVendor\":\"Temurin\""))
    }
}

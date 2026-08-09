package com.soumyajit.gradlemc.diagnostics

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagnosticSnapshotModelTest {
    @Test
    fun `memory presentation converts byte evidence without inventing native memory`() {
        val memory = MemorySnapshot(128L * 1024 * 1024, 256L * 1024 * 1024, 512L * 1024 * 1024, 384L * 1024 * 1024,
            25.0, DiagnosticSeverity.PASS, Instant.EPOCH)
        assertEquals(128, memory.usedMiB)
        assertEquals(384, memory.freeMiB)
    }

    @Test
    fun `check summary retains severity counts`() {
        val result = StabilityCheckResult(Instant.EPOCH, listOf(
            StabilityFinding(DiagnosticSeverity.PASS, "runtime", "ok"),
            StabilityFinding(DiagnosticSeverity.WARN, "memory", "high"),
        ))
        assertEquals(DiagnosticSeverity.WARN, result.highestSeverity)
        assertTrue(result.summary.contains("1 pass"))
        assertTrue(result.summary.contains("1 warn"))
    }
}

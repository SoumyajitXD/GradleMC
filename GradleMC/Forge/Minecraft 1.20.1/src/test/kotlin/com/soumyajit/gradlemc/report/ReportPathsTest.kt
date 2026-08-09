package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportPathsTest {
    @Test
    fun `report path is contained and does not create directories`() {
        val gameDirectory = Files.createTempDirectory("gradlemc-game-")
        val reports = ReportPaths.reportDirectory(gameDirectory, GradleMCConfigSnapshot.defaults())

        assertEquals(gameDirectory.resolve("gradlemc").resolve("reports").normalize(), reports)
        assertFalse(Files.exists(reports))
        assertEquals(reports.resolve("nested-output").normalize(), ReportPaths.resolveReportChild(reports, "nested-output"))
        assertEquals(reports.resolve("reports"), ReportPaths.resolveReportChild(reports.resolve("child").resolve(".."), "reports"))
    }

    @Test
    fun `unsafe path segments are rejected while unicode segments work`() {
        val base = Files.createTempDirectory("gradlemc-reports-")
        assertTrue(ReportPaths.isSafeFilenameSegment("résumé"))
        assertFailsWith<IllegalArgumentException> { ReportPaths.resolveReportChild(base, "..") }
        assertFailsWith<IllegalArgumentException> { ReportPaths.resolveReportChild(base, "nested/reports") }
        assertFailsWith<IllegalArgumentException> { ReportPaths.resolveReportChild(base, "C:\\private") }
        assertFailsWith<IllegalArgumentException> { ReportPaths.resolveReportChild(base, "\\\\server\\share") }
        assertFailsWith<IllegalArgumentException> { ReportPaths.resolveReportChild(base, "report?.txt") }
        assertFailsWith<IllegalArgumentException> { ReportPaths.resolveReportChild(base, "report. ") }
        assertFailsWith<IllegalArgumentException> { ReportPaths.resolveReportChild(base, " ") }
        assertFailsWith<IllegalArgumentException> { ReportPaths.resolveReportChild(base, "report\u0000.txt") }
    }
}

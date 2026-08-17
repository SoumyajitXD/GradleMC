package com.soumyajit.gradlemc.report

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReportFilesTest {
    @TempDir lateinit var temp: Path
    @Test fun `writes UTF-8 collision-safe reports and locates newest`() {
        val at = Instant.parse("2026-08-13T12:00:00Z")
        val first = ReportFiles.write(temp, "txt", "GradleMC ✓", at)
        val second = ReportFiles.write(temp, "txt", "{\"loader\":\"Fabric\"}", at)
        assertTrue(first.fileName.toString().endsWith(".txt")); assertTrue(second.fileName.toString().contains("-2.txt"))
        assertEquals("GradleMC ✓", Files.readString(first)); assertEquals(second, ReportFiles.latest(temp))
    }
    @Test fun `ignores unrelated files`() { Files.writeString(temp.resolve("notes.txt"), "x"); assertEquals(null, ReportFiles.latest(temp)); assertNotNull(ReportFiles.write(temp,"txt","x")) }
    @Test fun `lists bounded owned reports newest first`() {
        val first = ReportFiles.write(temp, "txt", "first", Instant.parse("2026-08-13T12:00:00Z"))
        val second = ReportFiles.write(temp, "json", "{}", Instant.parse("2026-08-13T12:01:00Z"))
        Files.writeString(temp.resolve("unrelated.json"), "{}")
        assertEquals(listOf(second), ReportFiles.list(temp, 1))
        assertEquals(listOf(second, first), ReportFiles.list(temp, 12))
    }
}

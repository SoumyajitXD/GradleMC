package com.soumyajit.gradlemc.report

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReportNameGeneratorTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-30T17:13:00Z"), ZoneId.of("UTC"))

    @Test
    fun `fixed clock and zone produce a stable locale independent filename`() {
        val generator = ReportNameGenerator(clock, ZoneId.of("Asia/Kolkata"))
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            assertEquals("gradlemc-report-20260730-224300.txt", generator.fileName())
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `extension is present once and unsafe inputs are rejected`() {
        val generator = ReportNameGenerator(clock, ZoneId.of("UTC"))
        assertTrue(generator.fileName(extension = ".TXT").endsWith(".txt"))
        assertFailsWith<IllegalArgumentException> { generator.fileName(prefix = "") }
        assertFailsWith<IllegalArgumentException> { generator.fileName(prefix = "bad/name-") }
        assertFailsWith<IllegalArgumentException> { generator.fileName(prefix = "report.txt") }
        assertFailsWith<IllegalArgumentException> { generator.fileName(extension = ".txt.txt") }
    }

    @Test
    fun `collision candidates are deterministic bounded and are filenames`() {
        val directory = Files.createTempDirectory("gradlemc-names-")
        val generator = ReportNameGenerator(clock, ZoneId.of("UTC"))
        val base = generator.fileName()
        val second = generator.fileName(collisionNumber = 2)
        assertEquals("gradlemc-report-20260730-171300.txt", base)
        assertEquals("gradlemc-report-20260730-171300-2.txt", second)
        assertEquals(second, generator.selectAvailable(directory, exists = { it.fileName.toString() == base }).fileName.toString())
        assertEquals(null, java.nio.file.Path.of(second).parent)
        assertTrue(generator.fileName(prefix = "x".repeat(200)).length <= ReportNamingDefaults.MAX_FILENAME_LENGTH)
        assertFailsWith<IllegalStateException> {
            generator.selectAvailable(directory, exists = { true })
        }
    }
}

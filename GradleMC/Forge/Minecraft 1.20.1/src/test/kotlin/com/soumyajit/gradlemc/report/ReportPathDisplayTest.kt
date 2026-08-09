package com.soumyajit.gradlemc.report

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportPathDisplayTest {
    @Test
    fun `paths beneath game directory are presented relatively`() {
        val game = Files.createTempDirectory("gradlemc-display-").toAbsolutePath()
        val report = game.resolve("gradlemc").resolve("reports").resolve("gradlemc-report.txt")

        val displayed = ReportPathDisplay.relativeToGame(game, report)
        assertEquals(game.relativize(report).toString(), displayed)
        assertFalse(displayed.contains(game.toString()))
    }

    @Test
    fun `redaction removes game and user home roots without changing ordinary text`() {
        val game = Files.createTempDirectory("gradlemc-display-").toAbsolutePath()
        val home = System.getProperty("user.home")
        val raw = "failed at ${game.resolve("gradlemc")} and ${java.nio.file.Path.of(home).resolve("private")}"

        val redacted = ReportPathDisplay.redact(raw, game)
        assertTrue(redacted.contains("[game-dir]"))
        assertTrue(redacted.contains("[user-home]"))
        assertFalse(redacted.contains(game.toString(), ignoreCase = true))
        assertFalse(redacted.contains(home, ignoreCase = true))
    }
}

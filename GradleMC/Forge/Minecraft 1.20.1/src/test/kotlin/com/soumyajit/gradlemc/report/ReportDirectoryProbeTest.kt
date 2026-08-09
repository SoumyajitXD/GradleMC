package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import java.nio.file.Files
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReportDirectoryProbeTest {
    @Test
    fun `fresh writable game directory is ready without creating output`() {
        val game = Files.createTempDirectory("gradlemc-probe-")
        val result = assertIs<ReportDirectoryReadiness.Ready>(ReportDirectoryProbe.inspect(game, GradleMCConfigSnapshot.defaults()))

        assertFalse(result.alreadyExists)
        assertFalse(Files.exists(game.resolve("gradlemc")))
    }

    @Test
    fun `existing report directory is ready`() {
        val game = Files.createTempDirectory("gradlemc-probe-")
        game.resolve("gradlemc").createDirectory().resolve("reports").createDirectory()

        val result = assertIs<ReportDirectoryReadiness.Ready>(ReportDirectoryProbe.inspect(game, GradleMCConfigSnapshot.defaults()))
        assertTrue(result.alreadyExists)
    }

    @Test
    fun `file where output root belongs is rejected`() {
        val game = Files.createTempDirectory("gradlemc-probe-")
        game.resolve("gradlemc").writeText("sentinel")

        val result = assertIs<ReportDirectoryReadiness.Failure>(ReportDirectoryProbe.inspect(game, GradleMCConfigSnapshot.defaults()))
        assertTrue(result.message.contains("not a safe directory"))
    }
}

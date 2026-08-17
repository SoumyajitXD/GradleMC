package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMcConfigSnapshot
import java.nio.file.Path
import java.nio.file.Files
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReportPathsTest {
    @TempDir lateinit var temp: Path
    @Test fun `reports remain below GradleMC game directory`() {
        assertEquals(Path.of("C:/game/gradlemc/reports"), ReportPaths.reportDirectory(Path.of("C:/game"), GradleMcConfigSnapshot()))
    }
    @Test fun `rejects a symbolic GradleMC output ancestor`() {
        val game = temp.resolve("game"); val outside = temp.resolve("outside")
        Files.createDirectories(game); Files.createDirectories(outside)
        try { Files.createSymbolicLink(game.resolve("gradlemc"), outside) } catch (_: UnsupportedOperationException) { return } catch (_: java.nio.file.FileSystemException) { return }
        assertFailsWith<IllegalArgumentException> { ReportPaths.reportDirectory(game, GradleMcConfigSnapshot()) }
    }
}

package com.soumyajit.gradlemc.command

import com.mojang.brigadier.CommandDispatcher
import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import com.soumyajit.gradlemc.diagnostics.DiagnosticSeverity
import com.soumyajit.gradlemc.diagnostics.DiagnosticSnapshot
import com.soumyajit.gradlemc.diagnostics.EnvironmentSnapshot
import com.soumyajit.gradlemc.diagnostics.LatestReportResult
import com.soumyajit.gradlemc.diagnostics.MemorySnapshot
import com.soumyajit.gradlemc.diagnostics.ReportExportResult
import com.soumyajit.gradlemc.diagnostics.SmartDiagnosticsResult
import com.soumyajit.gradlemc.diagnostics.StabilityCheckResult
import com.soumyajit.gradlemc.diagnostics.StabilityFinding
import com.soumyajit.gradlemc.performance.PerformanceMode
import com.soumyajit.gradlemc.performance.PerformanceSnapshot
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GradleMcCommandsExecutionTest {
    @Test
    fun `root and help provide the complete readable command overview`() {
        listOf("gradlemc", "gradlemc help").forEach { command ->
            val output = RecordingCommandSource()
            val dispatcher = dispatcher(FakeDiagnosticsBackend())

            assertEquals(1, dispatcher.execute(command, TestCommandSourceStack(output, 0)))
            assertTrue(output.messages.first().contains("diagnostics commands"))
            assertTrue(output.messages.any { it.contains("reports latest") })
            assertTrue(output.messages.any { it.contains("permission level 2") })
        }
    }

    @Test
    fun `gui command rejects a non-player source with the real failure message`() {
        val output = RecordingCommandSource()
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        GradleMcCommands.register(dispatcher)

        val result = dispatcher.execute("gradlemc gui", TestCommandSourceStack(output, 4))

        assertEquals(0, result)
        assertEquals(listOf("The GradleMC GUI can only be opened for an in-game player."), output.messages)
    }

    @Test
    fun `report export requires operator permission`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        GradleMcCommands.register(dispatcher)
        val export = dispatcher.root.getChild(CommandVocabulary.ROOT_COMMAND).getChild(CommandVocabulary.EXPORT)

        assertFalse(export.canUse(TestCommandSourceStack(RecordingCommandSource(), 0)))
        assertTrue(export.canUse(TestCommandSourceStack(RecordingCommandSource(), 2)))
    }

    @Test
    fun `operator restrictions cover every command that writes or changes diagnostic state`() {
        val dispatcher = dispatcher(FakeDiagnosticsBackend())
        val root = dispatcher.root.getChild(CommandVocabulary.ROOT_COMMAND)
        val restricted = listOf(
            root.getChild(CommandVocabulary.CHECK),
            root.getChild(CommandVocabulary.EXPORT),
            root.getChild(CommandVocabulary.SMART),
            root.getChild(CommandVocabulary.PERF),
            root.getChild(CommandVocabulary.PERFORMANCE).getChild(CommandVocabulary.SELF_TEST),
            root.getChild(CommandVocabulary.PERFORMANCE).getChild(CommandVocabulary.MODE).getChild(CommandVocabulary.LOW_IMPACT),
            root.getChild(CommandVocabulary.PERFORMANCE).getChild(CommandVocabulary.MODE).getChild(CommandVocabulary.BALANCED),
            root.getChild(CommandVocabulary.PERFORMANCE).getChild(CommandVocabulary.MODE).getChild(CommandVocabulary.DETAILED),
        )
        restricted.forEach { node ->
            assertFalse(node.canUse(TestCommandSourceStack(RecordingCommandSource(), 0)), node.name)
            assertTrue(node.canUse(TestCommandSourceStack(RecordingCommandSource(), 2)), node.name)
        }
    }

    @Test
    fun `bare reports branch explains its usable subcommand`() {
        val output = RecordingCommandSource()
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        GradleMcCommands.register(dispatcher)

        assertEquals(1, dispatcher.execute("gradlemc reports", TestCommandSourceStack(output, 0)))
        assertTrue(output.messages.single().contains("reports latest"))
    }

    @Test
    fun `core diagnostics commands render controlled readable evidence`() {
        val backend = FakeDiagnosticsBackend()
        val expectations = mapOf(
            "gradlemc version" to listOf("GradleMC version: test-version", "Environment: TEST"),
            "gradlemc status" to listOf("GradleMC status: dedicated_server", "Latest report: gradlemc\\reports\\gradlemc-report-test.txt"),
            "gradlemc memory" to listOf("GradleMC memory: 128 MiB", "pressure: warn"),
            "gradlemc check" to listOf("highest severity warn", "[WARN] Runtime memory snapshot"),
            "gradlemc export" to listOf("diagnostics export written", "gradlemc\\reports\\gradlemc-report-test.txt"),
            "gradlemc reports latest" to listOf("Latest GradleMC report", "gradlemc\\reports\\gradlemc-report-test.txt"),
            "gradlemc smart score" to listOf("score: 90/100", "implemented shallow checks"),
            "gradlemc smart advice" to listOf("score 90/100", "Review memory pressure", "implemented shallow checks"),
        )

        expectations.forEach { (command, fragments) ->
            val output = RecordingCommandSource()
            val result = dispatcher(backend).execute(command, TestCommandSourceStack(output, 2))
            assertEquals(1, result, command)
            val rendered = output.messages.joinToString("\n")
            fragments.forEach { fragment -> assertTrue(rendered.contains(fragment), "$command missing '$fragment': $rendered") }
        }
    }

    @Test
    fun `diagnostic failures return zero with actionable messages`() {
        val backend = FakeDiagnosticsBackend(
            exportResult = ReportExportResult.Failure("Report export failed: report directory is not writable"),
            latestResult = LatestReportResult.Empty,
        )

        val exportOutput = RecordingCommandSource()
        assertEquals(0, dispatcher(backend).execute("gradlemc export", TestCommandSourceStack(exportOutput, 2)))
        assertEquals(listOf("Report export failed: report directory is not writable"), exportOutput.messages)

        val latestOutput = RecordingCommandSource()
        assertEquals(0, dispatcher(backend).execute("gradlemc reports latest", TestCommandSourceStack(latestOutput, 0)))
        assertTrue(latestOutput.messages.single().contains("Run /gradlemc export"))
    }

    @Test
    fun `non-mutating branch help is available without operator permission`() {
        val expectations = mapOf(
            "gradlemc reports" to "reports latest",
            "gradlemc testfps" to "testfps start",
        )
        expectations.forEach { (command, expected) ->
            val output = RecordingCommandSource()
            assertEquals(1, dispatcher(FakeDiagnosticsBackend()).execute(command, TestCommandSourceStack(output, 0)))
            assertTrue(output.messages.single().contains(expected))
        }
    }

    private fun dispatcher(backend: DiagnosticsCommandBackend): CommandDispatcher<CommandSourceStack> =
        CommandDispatcher<CommandSourceStack>().also { GradleMcCommands.register(it, backend) }
}

private class FakeDiagnosticsBackend(
    private val exportResult: ReportExportResult = ReportExportResult.Success(REPORT, fixtureSnapshot(), DISPLAY_PATH),
    private val latestResult: LatestReportResult = LatestReportResult.Found(REPORT, DISPLAY_PATH),
) : DiagnosticsCommandBackend {
    override fun snapshot(): DiagnosticSnapshot = fixtureSnapshot()
    override fun runChecks(): StabilityCheckResult = fixtureChecks()
    override fun exportReport(): ReportExportResult = exportResult
    override fun latestReportResult(): LatestReportResult = latestResult
    override fun smartScore(): SmartDiagnosticsResult = smart()
    override fun smartAdvice(): SmartDiagnosticsResult = smart()
    override fun displayPath(path: Path): String = DISPLAY_PATH
    override fun versionLines(): List<String> = listOf("GradleMC version: test-version", "Environment: TEST")

    private fun smart() = SmartDiagnosticsResult(
        90,
        listOf("Review memory pressure."),
        "This score uses implemented shallow checks.",
        fixtureChecks(),
    )

    companion object {
        private val REPORT: Path = Path.of("C:/game/gradlemc/reports/gradlemc-report-test.txt")
        private const val DISPLAY_PATH = "gradlemc\\reports\\gradlemc-report-test.txt"

        private fun fixtureChecks() = StabilityCheckResult(
            Instant.parse("2026-08-02T01:02:03Z"),
            listOf(StabilityFinding(DiagnosticSeverity.WARN, "Runtime memory snapshot", "Used 128 MiB.", "Review memory pressure.")),
        )

        private fun fixtureSnapshot() = DiagnosticSnapshot(
            Instant.parse("2026-08-02T01:02:03Z"),
            EnvironmentSnapshot("test-version", "1.20.1", "47.4.22", "17", "Test OS", "amd64", "DEDICATED_SERVER", 3),
            MemorySnapshot(128L shl 20, 256L shl 20, 512L shl 20, 384L shl 20, 25.0, DiagnosticSeverity.WARN,
                Instant.parse("2026-08-02T01:02:03Z")),
            PerformanceSnapshot(PerformanceMode.BALANCED, null, null, 0, "No samples.", 5, 2),
            GradleMCConfigSnapshot.defaults(),
            REPORT,
            fixtureChecks(),
        )
    }
}

private class RecordingCommandSource : CommandSource {
    val messages = mutableListOf<String>()

    override fun sendSystemMessage(message: Component) {
        messages += message.string
    }

    override fun acceptsSuccess(): Boolean = true
    override fun acceptsFailure(): Boolean = true
    override fun shouldInformAdmins(): Boolean = false
}

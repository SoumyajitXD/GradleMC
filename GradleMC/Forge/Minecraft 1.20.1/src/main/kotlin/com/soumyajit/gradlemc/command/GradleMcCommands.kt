package com.soumyajit.gradlemc.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.soumyajit.gradlemc.GradleMC
import com.soumyajit.gradlemc.diagnostics.DiagnosticsService
import com.soumyajit.gradlemc.diagnostics.DiagnosticSnapshot
import com.soumyajit.gradlemc.diagnostics.LatestReportResult
import com.soumyajit.gradlemc.diagnostics.ReportExportResult
import com.soumyajit.gradlemc.diagnostics.SmartDiagnosticsResult
import com.soumyajit.gradlemc.diagnostics.StabilityCheckResult
import com.soumyajit.gradlemc.network.GradleMcNetwork
import com.soumyajit.gradlemc.performance.FpsTestService
import com.soumyajit.gradlemc.performance.PerformanceMode
import com.soumyajit.gradlemc.performance.PerformanceService
import net.minecraft.SharedConstants
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.versions.forge.ForgeVersion

internal interface DiagnosticsCommandBackend {
    fun snapshot(): DiagnosticSnapshot
    fun runChecks(): StabilityCheckResult
    fun exportReport(): ReportExportResult
    fun latestReportResult(): LatestReportResult
    fun smartScore(): SmartDiagnosticsResult
    fun smartAdvice(): SmartDiagnosticsResult
    fun displayPath(path: java.nio.file.Path): String
    fun versionLines(): List<String>
}

private object RuntimeDiagnosticsCommandBackend : DiagnosticsCommandBackend {
    override fun snapshot(): DiagnosticSnapshot = DiagnosticsService.snapshot()
    override fun runChecks(): StabilityCheckResult = DiagnosticsService.runChecks()
    override fun exportReport(): ReportExportResult = DiagnosticsService.exportReport()
    override fun latestReportResult(): LatestReportResult = DiagnosticsService.latestReportResult()
    override fun smartScore(): SmartDiagnosticsResult = DiagnosticsService.smartScore()
    override fun smartAdvice(): SmartDiagnosticsResult = DiagnosticsService.smartAdvice()
    override fun displayPath(path: java.nio.file.Path): String = DiagnosticsService.displayPath(path)
    override fun versionLines(): List<String> {
        val modVersion = loadedModVersion(GradleMC.MOD_ID, GradleMC.VERSION)
        val kotlinForForgeVersion = loadedModVersion(GradleMC.KOTLIN_FOR_FORGE_MOD_ID, "unknown")
        return listOf(
            "GradleMC version: $modVersion",
            "Minecraft: ${SharedConstants.getCurrentVersion().name}",
            "Forge: ${ForgeVersion.getVersion()}",
            "Java runtime: ${System.getProperty("java.version")}",
            "Kotlin runtime: ${KotlinVersion.CURRENT}",
            "Kotlin for Forge: $kotlinForForgeVersion",
            "Environment: ${environmentLabel(FMLEnvironment.dist)}",
        )
    }

    private fun loadedModVersion(modId: String, fallback: String): String =
        ModList.get().getModContainerById(modId).map { it.modInfo.version.toString() }.orElse(fallback)

    private fun environmentLabel(dist: Dist): String = when (dist) {
        Dist.CLIENT -> "CLIENT"
        Dist.DEDICATED_SERVER -> "DEDICATED_SERVER"
    }
}

/**
 * The small, usable command surface shared by dedicated servers and integrated clients.
 *
 * Client screen classes never appear here. The GUI command sends its requesting player a
 * server-to-client payload; the packet is handled by the client-owned GUI bridge.
 */
object GradleMcCommands {
    private const val MIN_SAMPLE_SECONDS = 5
    private const val MAX_SAMPLE_SECONDS = 1_800

    fun register(event: RegisterCommandsEvent) {
        register(event.dispatcher, RuntimeDiagnosticsCommandBackend)
    }

    /** Separate from Forge's event solely to make the registered shape unit-testable. */
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        register(dispatcher, RuntimeDiagnosticsCommandBackend)
    }

    internal fun register(dispatcher: CommandDispatcher<CommandSourceStack>, diagnostics: DiagnosticsCommandBackend) {
        val root = Commands.literal(CommandVocabulary.ROOT_COMMAND)
            .executes { context -> help(context.source) }
            .then(Commands.literal(CommandVocabulary.HELP).executes { context -> help(context.source) })
            .then(Commands.literal(CommandVocabulary.GUI).executes { context -> openGui(context.source) })
            .then(Commands.literal(CommandVocabulary.STATUS).executes { context -> status(context.source, diagnostics) })
            .then(Commands.literal(CommandVocabulary.VERSION_SUBCOMMAND).executes { context -> version(context.source, diagnostics) })
            .then(Commands.literal(CommandVocabulary.MEMORY).executes { context -> memory(context.source, diagnostics) })
            .then(Commands.literal(CommandVocabulary.CHECK)
                .requires { source -> source.hasPermission(2) }
                .executes { context -> check(context.source, diagnostics) })
            .then(Commands.literal(CommandVocabulary.EXPORT)
                .requires { source -> source.hasPermission(2) }
                .executes { context -> export(context.source, diagnostics) })
            .then(reportsBranch(diagnostics))
            .then(smartBranch(diagnostics))
            .then(fpsTestBranch())
            .then(perfBranch())
            .then(performanceBranch())

        dispatcher.register(root)
        GradleMC.LOGGER.info("Registered /{} diagnostics command tree", CommandVocabulary.ROOT_COMMAND)
    }

    private fun reportsBranch(diagnostics: DiagnosticsCommandBackend) = Commands.literal(CommandVocabulary.REPORTS)
        .executes { context -> success(context.source, "Use /gradlemc reports latest to locate the newest local report.") }
        .then(Commands.literal(CommandVocabulary.LATEST).executes { context -> latestReport(context.source, diagnostics) })

    private fun smartBranch(diagnostics: DiagnosticsCommandBackend) = Commands.literal(CommandVocabulary.SMART)
        .requires { source -> source.hasPermission(2) }
        .executes { context -> smartHelp(context.source) }
        .then(Commands.literal(CommandVocabulary.SCORE).executes { context -> smartScore(context.source, diagnostics) })
        .then(Commands.literal(CommandVocabulary.ADVICE).executes { context -> smartAdvice(context.source, diagnostics) })

    private fun fpsTestBranch() = Commands.literal(CommandVocabulary.TEST_FPS)
        .executes { context -> success(context.source, "Use /gradlemc testfps start <seconds> or /gradlemc testfps stop in an integrated world.") }
        .then(
            Commands.literal(CommandVocabulary.START)
                .then(Commands.argument("seconds", IntegerArgumentType.integer(MIN_SAMPLE_SECONDS, MAX_SAMPLE_SECONDS))
                    .executes { context ->
                        val seconds = IntegerArgumentType.getInteger(context, "seconds")
                        fpsStart(context.source, seconds)
                    })
        )
        .then(Commands.literal(CommandVocabulary.STOP).executes { context -> fpsStop(context.source) })

    private fun perfBranch() = Commands.literal(CommandVocabulary.PERF)
        .requires { source -> source.hasPermission(2) }
        .executes { context -> perfHelp(context.source) }
        .then(
            Commands.literal(CommandVocabulary.START)
                .then(Commands.argument("seconds", IntegerArgumentType.integer(MIN_SAMPLE_SECONDS, MAX_SAMPLE_SECONDS))
                    .executes { context -> perfStart(context.source, IntegerArgumentType.getInteger(context, "seconds")) })
        )
        .then(Commands.literal(CommandVocabulary.STOP).executes { context -> perfStop(context.source) })
        // Donor compatibility: `/gradlemc perf 60` means `/gradlemc perf start 60`.
        .then(Commands.argument("seconds", IntegerArgumentType.integer(MIN_SAMPLE_SECONDS, MAX_SAMPLE_SECONDS))
            .executes { context -> perfStart(context.source, IntegerArgumentType.getInteger(context, "seconds")) })

    private fun performanceBranch() = Commands.literal(CommandVocabulary.PERFORMANCE)
        .executes { context -> performanceSummary(context.source) }
        .then(Commands.literal(CommandVocabulary.OVERHEAD).executes { context -> performanceOverhead(context.source) })
        .then(Commands.literal(CommandVocabulary.GUARD).executes { context -> performanceGuard(context.source) })
        .then(Commands.literal(CommandVocabulary.EXPLAIN).executes { context -> performanceExplain(context.source) })
        .then(Commands.literal(CommandVocabulary.SELF_TEST)
            .requires { source -> source.hasPermission(2) }
            .executes { context -> performanceSelfTest(context.source) })
        .then(
            Commands.literal(CommandVocabulary.MODE)
                .executes { context -> performanceSummary(context.source) }
                .then(modeLiteral(CommandVocabulary.LOW_IMPACT, PerformanceMode.LOW_IMPACT))
                .then(modeLiteral(CommandVocabulary.BALANCED, PerformanceMode.BALANCED))
                .then(modeLiteral(CommandVocabulary.DETAILED, PerformanceMode.DETAILED))
        )

    private fun modeLiteral(word: String, mode: PerformanceMode) = Commands.literal(word)
        .requires { source -> source.hasPermission(2) }
        .executes { context -> setPerformanceMode(context.source, mode) }

    private fun help(source: CommandSourceStack): Int = success(source, listOf(
        "GradleMC diagnostics commands:",
        "/gradlemc gui - open the diagnostics panel on this client.",
        "/gradlemc version | status | memory | check - inspect the current runtime.",
        "/gradlemc smart score | advice - run local rule-based diagnostics.",
        "/gradlemc reports latest - locate the newest local report.",
        "/gradlemc export - write a local report (permission level 2).",
        "/gradlemc testfps start <seconds> | stop - measure integrated-client rendered frames.",
        "/gradlemc perf start <seconds> | stop - measure server tick execution (permission level 2).",
        "/gradlemc performance - inspect sampling, modes, and GradleMC-owned overhead.",
    ))

    private fun openGui(source: CommandSourceStack): Int {
        val player = source.entity as? ServerPlayer
            ?: return failure(source, "The GradleMC GUI can only be opened for an in-game player.").also {
                GradleMC.LOGGER.info("/gradlemc gui rejected: command source is not a player")
            }
        GradleMC.LOGGER.debug("/gradlemc gui executed by {}", player.scoreboardName)
        GradleMcNetwork.openGui(player)
        return success(source, "Requested GradleMC diagnostics on your client.")
    }

    private fun status(source: CommandSourceStack, diagnostics: DiagnosticsCommandBackend): Int {
        val snapshot = diagnostics.snapshot()
        val environment = snapshot.environment
        return success(source, listOf(
            "GradleMC status: ${environment.physicalSide.lowercase()}, ${environment.installedModCount ?: "unknown"} mods, performance mode ${snapshot.performance.mode.label}.",
            "Memory: ${snapshot.memory.usedMiB} MiB / ${snapshot.memory.maxMiB} MiB (${formatPercent(snapshot.memory.usedPercent)}%, ${snapshot.memory.pressure.name.lowercase()}).",
            "Rendered FPS: ${formatFps(snapshot.performance.currentFps)}; average: ${formatFps(snapshot.performance.averageFps)}.",
            "Latest report: ${snapshot.latestReport?.let(diagnostics::displayPath) ?: "none"}.",
        ))
    }

    private fun memory(source: CommandSourceStack, diagnostics: DiagnosticsCommandBackend): Int {
        val memory = diagnostics.snapshot().memory
        return success(source, listOf(
            "GradleMC memory: ${memory.usedMiB} MiB used / ${memory.maxMiB} MiB max (${formatPercent(memory.usedPercent)}%).",
            "Committed: ${memory.committedMiB} MiB; available headroom: ${memory.freeMiB} MiB; pressure: ${memory.pressure.name.lowercase()}.",
        ))
    }

    private fun check(source: CommandSourceStack, diagnostics: DiagnosticsCommandBackend): Int {
        val checks = diagnostics.runChecks()
        return success(source, buildList {
            add("GradleMC checks: ${checks.summary}; highest severity ${checks.highestSeverity.name.lowercase()}.")
            checks.findings.filter { it.severity.ordinal >= com.soumyajit.gradlemc.diagnostics.DiagnosticSeverity.WARN.ordinal }
                .forEach { add("[${it.severity}] ${it.title}: ${it.detail}") }
            if (checks.findings.none { it.severity.ordinal >= com.soumyajit.gradlemc.diagnostics.DiagnosticSeverity.WARN.ordinal }) {
                add("No warning or failure findings were produced by the implemented checks.")
            }
        })
    }

    private fun export(source: CommandSourceStack, diagnostics: DiagnosticsCommandBackend): Int {
        val result = diagnostics.exportReport()
        return when (result) {
            is ReportExportResult.Success -> success(source, "GradleMC diagnostics export written: ${result.displayPath}")
            is ReportExportResult.Failure -> failure(source, result.message)
        }
    }

    private fun latestReport(source: CommandSourceStack, diagnostics: DiagnosticsCommandBackend): Int {
        return when (val latest = diagnostics.latestReportResult()) {
            is LatestReportResult.Found -> success(source, "Latest GradleMC report: ${latest.displayPath}")
            LatestReportResult.Empty -> failure(source, "No GradleMC reports found yet. Run /gradlemc export to create one.")
            is LatestReportResult.Failure -> failure(source, latest.message)
        }
    }

    private fun smartHelp(source: CommandSourceStack): Int = success(source, "Use /gradlemc smart score or /gradlemc smart advice.")

    private fun smartScore(source: CommandSourceStack, diagnostics: DiagnosticsCommandBackend): Int {
        val result = diagnostics.smartScore()
        return success(source, listOf(
            "GradleMC Smart Diagnostics score: ${result.score}/100.",
            result.message,
            "Checks: ${result.basedOn.summary}.",
        ))
    }

    private fun smartAdvice(source: CommandSourceStack, diagnostics: DiagnosticsCommandBackend): Int {
        val result = diagnostics.smartAdvice()
        return success(source, buildList {
            add("GradleMC Smart Diagnostics advice (score ${result.score}/100):")
            addAll(result.advice.map { "- $it" })
            add(result.message)
        })
    }

    private fun fpsStart(source: CommandSourceStack, seconds: Int): Int {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return failure(source, "Rendered FPS tests require an integrated client and are unavailable on a dedicated server.")
        }
        if (source.entity !is ServerPlayer) {
            return failure(source, "Rendered FPS tests can only be controlled by an in-game player.")
        }
        return action(source, FpsTestService.start(seconds))
    }

    private fun fpsStop(source: CommandSourceStack): Int {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return failure(source, "Rendered FPS tests are unavailable on a dedicated server.")
        }
        if (source.entity !is ServerPlayer) {
            return failure(source, "Rendered FPS tests can only be controlled by an in-game player.")
        }
        return action(source, FpsTestService.stop())
    }

    private fun perfHelp(source: CommandSourceStack): Int = success(source, "Use /gradlemc perf start <seconds>, /gradlemc perf <seconds>, or /gradlemc perf stop.")

    /** Compatibility alias for a bounded rendered-frame performance sample. */
    private fun perfStart(source: CommandSourceStack, seconds: Int): Int = action(source, PerformanceService.startTimedSample(seconds))

    private fun perfStop(source: CommandSourceStack): Int = action(source, PerformanceService.stopTimedSample())

    private fun performanceSummary(source: CommandSourceStack): Int {
        val value = PerformanceService.snapshot()
        return success(source, listOf(
            "GradleMC performance: mode=${value.mode.label}, frames=${value.observedFrames}.",
            "Current FPS: ${formatFps(value.currentFps)}; average FPS: ${formatFps(value.averageFps)}.",
            value.message,
        ))
    }

    private fun performanceOverhead(source: CommandSourceStack): Int = success(source, PerformanceService.overheadDescription())

    private fun performanceGuard(source: CommandSourceStack): Int = success(source, PerformanceService.guardDescription())

    private fun performanceExplain(source: CommandSourceStack): Int = success(source, listOf(
        PerformanceService.explainDescription(),
        "Mode controls GradleMC collection detail: low impact, balanced, or detailed."
    ))

    private fun performanceSelfTest(source: CommandSourceStack): Int = success(
        source,
        if (PerformanceService.selfTest()) "GradleMC performance self-test passed: a 16.67 ms rendered frame resolves to 60 FPS."
        else "GradleMC performance self-test failed; FPS output should not be trusted."
    )

    private fun setPerformanceMode(source: CommandSourceStack, mode: PerformanceMode): Int {
        val result = PerformanceService.setMode(mode)
        return if (result.success) success(source, result.message) else failure(source, result.message)
    }

    private fun version(source: CommandSourceStack, diagnostics: DiagnosticsCommandBackend): Int =
        success(source, diagnostics.versionLines())

    private fun success(source: CommandSourceStack, message: String): Int = success(source, listOf(message))

    private fun success(source: CommandSourceStack, messages: Iterable<String>): Int {
        messages.forEach { message -> source.sendSuccess({ Component.literal(message) }, false) }
        return Command.SINGLE_SUCCESS
    }

    private fun failure(source: CommandSourceStack, message: String): Int {
        source.sendFailure(Component.literal(message))
        return 0
    }

    private fun action(source: CommandSourceStack, result: com.soumyajit.gradlemc.performance.FpsTestActionResult): Int =
        if (result.success) success(source, result.message) else failure(source, result.message)

    private fun formatFps(value: Double?): String = value?.let { "%.1f".format(java.util.Locale.ROOT, it) } ?: "unavailable"

    private fun formatPercent(value: Double): String = "%.1f".format(java.util.Locale.ROOT, value)

}

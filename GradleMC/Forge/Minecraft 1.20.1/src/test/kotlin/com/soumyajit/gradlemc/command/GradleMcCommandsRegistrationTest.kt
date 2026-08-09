package com.soumyajit.gradlemc.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GradleMcCommandsRegistrationTest {
    @Test
    fun `registers the required lowercase command surface`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        GradleMcCommands.register(dispatcher)

        val root = assertNotNull(dispatcher.root.getChild(CommandVocabulary.ROOT_COMMAND))
        listOf(
            CommandVocabulary.HELP,
            CommandVocabulary.GUI,
            CommandVocabulary.STATUS,
            CommandVocabulary.VERSION_SUBCOMMAND,
            CommandVocabulary.MEMORY,
            CommandVocabulary.CHECK,
            CommandVocabulary.EXPORT,
            CommandVocabulary.REPORTS,
            CommandVocabulary.SMART,
            CommandVocabulary.TEST_FPS,
            CommandVocabulary.PERF,
            CommandVocabulary.PERFORMANCE,
        ).forEach { assertNotNull(root.getChild(it), "Missing /gradlemc $it") }

        assertNotNull(root.getChild(CommandVocabulary.REPORTS)?.getChild(CommandVocabulary.LATEST))
        assertNotNull(root.getChild(CommandVocabulary.SMART)?.getChild(CommandVocabulary.SCORE))
        assertNotNull(root.getChild(CommandVocabulary.SMART)?.getChild(CommandVocabulary.ADVICE))
        assertNotNull(root.getChild(CommandVocabulary.TEST_FPS)?.getChild(CommandVocabulary.START))
        assertNotNull(root.getChild(CommandVocabulary.TEST_FPS)?.getChild(CommandVocabulary.STOP))

        val performance = assertNotNull(root.getChild(CommandVocabulary.PERFORMANCE))
        listOf(CommandVocabulary.OVERHEAD, CommandVocabulary.GUARD, CommandVocabulary.EXPLAIN, CommandVocabulary.SELF_TEST, CommandVocabulary.MODE)
            .forEach { assertNotNull(performance.getChild(it), "Missing /gradlemc performance $it") }
        val mode = assertNotNull(performance.getChild(CommandVocabulary.MODE))
        listOf(CommandVocabulary.LOW_IMPACT, CommandVocabulary.BALANCED, CommandVocabulary.DETAILED)
            .forEach { assertNotNull(mode.getChild(it), "Missing /gradlemc performance mode $it") }
    }

    @Test
    fun `every registered command shape parses with its documented lowercase spelling`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        GradleMcCommands.register(dispatcher)
        val source = TestCommandSourceStack(RegistrationCommandSource, 4)
        val commands = listOf(
            "gradlemc", "gradlemc help", "gradlemc gui", "gradlemc status", "gradlemc version",
            "gradlemc memory", "gradlemc check", "gradlemc export", "gradlemc reports",
            "gradlemc reports latest", "gradlemc smart", "gradlemc smart score", "gradlemc smart advice",
            "gradlemc testfps", "gradlemc testfps start 5", "gradlemc testfps stop",
            "gradlemc perf", "gradlemc perf start 5", "gradlemc perf 5", "gradlemc perf stop",
            "gradlemc performance", "gradlemc performance overhead", "gradlemc performance guard",
            "gradlemc performance explain", "gradlemc performance selftest", "gradlemc performance mode",
            "gradlemc performance mode low_impact", "gradlemc performance mode balanced",
            "gradlemc performance mode detailed",
        )

        commands.forEach { command ->
            val parse = dispatcher.parse(command, source)
            assertFalse(parse.reader.canRead(), "Unparsed input for '$command': '${parse.reader.remaining}'")
            assertTrue(parse.exceptions.isEmpty(), "Parse exception for '$command': ${parse.exceptions.values}")
        }
    }

    @Test
    fun `uppercase and out of range forms do not parse as valid commands`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        GradleMcCommands.register(dispatcher)
        val source = TestCommandSourceStack(RegistrationCommandSource, 4)

        listOf(
            "GradleMC", "gradlemc STATUS", "gradlemc testfps start 4", "gradlemc testfps start 1801",
            "gradlemc perf start 4", "gradlemc perf 1801", "gradlemc performance mode LOW_IMPACT",
        ).forEach { command ->
            val parse = dispatcher.parse(command, source)
            assertTrue(parse.reader.canRead() || parse.exceptions.isNotEmpty(), "Unexpectedly accepted '$command'")
        }
    }
}

private object RegistrationCommandSource : net.minecraft.commands.CommandSource {
    override fun sendSystemMessage(message: net.minecraft.network.chat.Component) = Unit
    override fun acceptsSuccess(): Boolean = true
    override fun acceptsFailure(): Boolean = true
    override fun shouldInformAdmins(): Boolean = false
}

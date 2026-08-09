package com.soumyajit.gradlemc.command

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandVocabularyTest {
    @Test
    fun `root command is the stable lowercase mod id`() {
        assertEquals("gradlemc", CommandVocabulary.ROOT_COMMAND)
        assertEquals(CommandVocabulary.ROOT_COMMAND, CommandVocabulary.ROOT_COMMAND.lowercase())
    }

    @Test
    fun `version subcommand remains lowercase and nonblank`() {
        assertEquals("version", CommandVocabulary.VERSION_SUBCOMMAND)
        assertEquals(CommandVocabulary.VERSION_SUBCOMMAND, CommandVocabulary.VERSION_SUBCOMMAND.lowercase())
        assertTrue(CommandVocabulary.VERSION_SUBCOMMAND.isNotBlank())
    }

    @Test
    fun `all public command words remain lowercase`() {
        assertTrue(CommandVocabulary.requiredCommands.all { word -> word == word.lowercase() && word.isNotBlank() })
    }
}

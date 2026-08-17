package com.soumyajit.gradlemc.command

import kotlin.test.Test
import kotlin.test.assertTrue

/** Guards the registered, lowercase public vocabulary without requiring a Minecraft server fixture. */
class GradleMcCommandsSurfaceTest {
    private val source = java.nio.file.Path.of("src/main/kotlin/com/soumyajit/gradlemc/command/GradleMcCommands.kt").toFile().readText()

    @Test fun `current Forge command vocabulary is registered`() {
        listOf("help", "gui", "version", "status", "memory", "check", "export", "reports", "latest", "smart", "score", "advice", "testfps", "perf", "performance", "overhead", "guard", "explain", "selftest", "mode", "low_impact", "balanced", "detailed").forEach { word ->
            assertTrue(source.contains("\"$word\""), "missing command literal $word")
        }
    }

    @Test fun `recovered command families and overlay controls are registered`() {
        listOf("overlay", "title", "fps", "average", "window", "reset", "issuebundle", "create", "files", "config", "path", "mods", "list", "search", "inspect", "audit", "entities", "blockentities", "thresholds").forEach { word ->
            assertTrue(source.contains("\"$word\""), "missing command literal $word")
        }
    }
}

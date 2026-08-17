package com.soumyajit.gradlemc

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse

class DedicatedServerBoundaryTest {
    @Test fun `common sources do not statically import client APIs`() {
        val forbidden = listOf("net.minecraft.client", "net.fabricmc.fabric.api.client")
        Files.walk(Path.of("src/main/kotlin")).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }.forEach { source ->
                val text = Files.readString(source)
                forbidden.forEach { api -> assertFalse(text.contains(api), "$source references client-only API $api") }
            }
        }
    }
}

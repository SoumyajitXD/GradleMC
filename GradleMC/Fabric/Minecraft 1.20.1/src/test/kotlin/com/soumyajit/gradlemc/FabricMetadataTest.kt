package com.soumyajit.gradlemc

import kotlin.test.Test
import kotlin.test.assertTrue

class FabricMetadataTest {
    @Test fun `metadata declares GradleMC Kotlin Fabric entrypoints and dependencies`() {
        val text = checkNotNull(javaClass.classLoader.getResource("fabric.mod.json")) { "fabric.mod.json is missing from test resources" }.readText()
        assertTrue(text.contains("\"id\": \"gradlemc\""))
        assertTrue(text.contains("\"value\": \"com.soumyajit.gradlemc.GradleMC\""))
        assertTrue(text.contains("\"value\": \"com.soumyajit.gradlemc.client.GradleMcClient\""))
        assertTrue(text.contains("\"adapter\": \"kotlin\""))
        assertTrue(text.contains("fabric-language-kotlin"))
    }
}

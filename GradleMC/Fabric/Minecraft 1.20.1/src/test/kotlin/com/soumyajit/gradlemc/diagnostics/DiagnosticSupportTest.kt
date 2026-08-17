package com.soumyajit.gradlemc.diagnostics

import com.soumyajit.gradlemc.config.GradleMcConfigSnapshot
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticSupportTest {
    @Test fun `config checker accepts supported settings`() {
        assertTrue(DiagnosticSupport.checkConfig(GradleMcConfigSnapshot()).valid)
    }

    @Test fun `config checker reports invalid values without reading arbitrary files`() {
        val result = DiagnosticSupport.checkConfig(GradleMcConfigSnapshot(reportDirectoryName = "..", overlaySamplingWindowSeconds = 7, performanceMode = "turbo"))
        assertFalse(result.valid)
        assertTrue(result.messages.size == 3)
    }
}

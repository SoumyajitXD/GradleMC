package com.soumyajit.gradlemc.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals

class OperatingSystemInfoTest {
    @Test fun `windows 11 is determined by build number`() {
        assertEquals("Windows 11 10.0.22631", OperatingSystemClassifier.classify("Windows 10", "10.0", "22631").displayName)
    }
    @Test fun `windows 10 remains below the Windows 11 build threshold`() {
        assertEquals("Windows 10 10.0.19045", OperatingSystemClassifier.classify("Windows", "10.0", "19045").displayName)
    }
}

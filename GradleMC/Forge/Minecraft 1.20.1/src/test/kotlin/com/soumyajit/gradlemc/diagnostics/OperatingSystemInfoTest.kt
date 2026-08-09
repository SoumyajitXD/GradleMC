package com.soumyajit.gradlemc.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OperatingSystemInfoTest {
    @Test
    fun `Windows 10 build remains Windows 10 with NT detail`() {
        val result = OperatingSystemClassifier.classify("Windows 10", "10.0", "19045")

        assertEquals("Windows 10 10.0.19045", result.displayName)
        assertEquals(19045, result.windowsBuild)
    }

    @Test
    fun `Windows 11 build threshold and later builds are classified from build number`() {
        listOf(22_000, 22_631, 26_100).forEach { build ->
            val result = OperatingSystemClassifier.classify("Windows 10", "10.0", build.toString())

            assertEquals("Windows 11 10.0.$build", result.displayName)
            assertEquals(build, result.windowsBuild)
        }
    }

    @Test
    fun `malformed or unavailable Windows build safely preserves JVM fallback`() {
        listOf(null, "", "not-a-build", "-1").forEach { build ->
            val result = OperatingSystemClassifier.classify("Windows 10", "10.0", build)

            assertEquals("Windows 10 10.0", result.displayName)
            assertNull(result.windowsBuild)
        }
    }

    @Test
    fun `non Windows operating systems pass through their JVM presentation`() {
        val result = OperatingSystemClassifier.classify("Linux", "6.12.0", "26100")

        assertEquals("Linux 6.12.0", result.displayName)
        assertNull(result.windowsBuild)
    }
}

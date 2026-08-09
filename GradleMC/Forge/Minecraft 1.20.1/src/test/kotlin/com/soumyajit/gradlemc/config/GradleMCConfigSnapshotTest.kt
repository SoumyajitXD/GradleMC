package com.soumyajit.gradlemc.config

import com.soumyajit.gradlemc.command.CommandVocabulary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GradleMCConfigSnapshotTest {
    @Test
    fun `defaults preserve the selected donor report behavior`() {
        assertEquals("reports", GradleMCConfigSnapshot.defaults().reportDirectoryName)
        assertEquals("reportDirectoryName", ForgeGradleMCConfig.REPORT_DIRECTORY_NAME_KEY)
        assertEquals("gradlemc", CommandVocabulary.ROOT_COMMAND)
    }

    @Test
    fun `snapshot equality represents configuration equality`() {
        assertEquals(
            GradleMCConfigSnapshot("reports"),
            GradleMCConfigSnapshot("reports"),
        )
        assertFalse(GradleMCConfigSnapshot("reports") == GradleMCConfigSnapshot("exports"))
    }

    @Test
    fun `directory name validation has stable boundaries`() {
        assertTrue(GradleMCConfigSnapshot.isValidReportDirectoryName("a"))
        assertTrue(GradleMCConfigSnapshot.isValidReportDirectoryName("a".repeat(64)))
        assertFalse(GradleMCConfigSnapshot.isValidReportDirectoryName(""))
        assertFalse(GradleMCConfigSnapshot.isValidReportDirectoryName("reports/name"))
        assertFalse(GradleMCConfigSnapshot.isValidReportDirectoryName(".."))
        assertFalse(GradleMCConfigSnapshot.isValidReportDirectoryName("reports."))
        assertFalse(GradleMCConfigSnapshot.isValidReportDirectoryName("reports "))
        listOf("CON", "con.txt", "NUL", "AUX", "PRN", "COM1", "LPT9").forEach { reserved ->
            assertFalse(GradleMCConfigSnapshot.isValidReportDirectoryName(reserved), "Accepted Windows device name $reserved")
        }
        assertFailsWith<IllegalArgumentException> { GradleMCConfigSnapshot(" ") }
    }

    @Test
    fun `snapshot remains a pure immutable value`() {
        val fields = GradleMCConfigSnapshot::class.java.declaredFields
        assertTrue(fields.none { it.type.name.startsWith("net.minecraftforge.") })
        assertTrue(fields.none { java.util.Collection::class.java.isAssignableFrom(it.type) || java.util.Map::class.java.isAssignableFrom(it.type) })
    }
}

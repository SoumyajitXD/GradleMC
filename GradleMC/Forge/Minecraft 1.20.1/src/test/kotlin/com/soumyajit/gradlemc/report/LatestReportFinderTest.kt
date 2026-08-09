package com.soumyajit.gradlemc.report

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText
import org.opentest4j.TestAbortedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LatestReportFinderTest {
    @Test
    fun `missing and empty directories are distinct from scan failures`() {
        val root = Files.createTempDirectory("gradlemc-latest-")
        assertIs<LatestReportLookup.Empty>(LatestReportFinder.find(root.resolve("missing")))
        assertIs<LatestReportLookup.Empty>(LatestReportFinder.find(root.resolve("empty").createDirectory()))
        assertIs<LatestReportLookup.Failure>(LatestReportFinder.find(root.resolve("not-directory").also { it.writeText("x") }))
    }

    @Test
    fun `latest selection uses modification time and deterministic collision tie break`() {
        val directory = Files.createTempDirectory("gradlemc-latest-")
        val first = directory.resolve("gradlemc-report-20260802-010203.txt").also { it.writeText("first") }
        val second = directory.resolve("gradlemc-report-20260802-010203-2.txt").also { it.writeText("second") }
        directory.resolve("gradlemc-report-malformed.txt").writeText("ignore")
        val sameTime = FileTime.fromMillis(123_456L)
        Files.setLastModifiedTime(first, sameTime)
        Files.setLastModifiedTime(second, sameTime)

        val found = assertIs<LatestReportLookup.Found>(LatestReportFinder.find(directory))
        assertEquals(second, found.path)
    }

    @Test
    fun `symbolic-link report candidates are ignored when links are supported`() {
        val directory = Files.createTempDirectory("gradlemc-latest-")
        val outside = Files.createTempFile("gradlemc-outside-", ".txt").also { it.writeText("outside") }
        val link = directory.resolve("gradlemc-report-20260802-010203.txt")
        try {
            Files.createSymbolicLink(link, outside)
        } catch (exception: UnsupportedOperationException) {
            throw TestAbortedException("Symbolic links are unsupported", exception)
        } catch (exception: java.nio.file.FileSystemException) {
            throw TestAbortedException("Symbolic-link creation is denied", exception)
        }

        assertIs<LatestReportLookup.Empty>(LatestReportFinder.find(directory))
    }

    @Test
    fun `directory scan fails safely when the donor entry bound is exceeded`() {
        val directory = Files.createTempDirectory("gradlemc-latest-")
        val report = directory.resolve("gradlemc-report-20260802-010203.txt").also { it.writeText("report") }
        repeat(LatestReportFinder.MAX_INSPECTED_ENTRIES - 1) { index ->
            directory.resolve("unrelated-$index.tmp").writeText("ignore")
        }

        assertEquals(report, assertIs<LatestReportLookup.Found>(LatestReportFinder.find(directory)).path)

        directory.resolve("overflow.tmp").writeText("ignore")
        val failure = assertIs<LatestReportLookup.Failure>(LatestReportFinder.find(directory))
        assertEquals("Report directory contains too many entries to scan safely", failure.message)
    }
}

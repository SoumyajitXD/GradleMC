package com.soumyajit.gradlemc.report

import java.io.IOException
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime

sealed interface LatestReportLookup {
    data class Found(val path: Path) : LatestReportLookup
    data object Empty : LatestReportLookup
    data class Failure(val message: String, val cause: Throwable? = null) : LatestReportLookup
}

/** Deterministic, no-follow lookup for reports directly inside one trusted directory. */
object LatestReportFinder {
    internal const val MAX_INSPECTED_ENTRIES = 256
    private val REPORT_NAME = Regex("^gradlemc-report-(\\d{8}-\\d{6})(?:-(\\d+))?\\.txt$")

    fun find(directory: Path): LatestReportLookup {
        val normalized = directory.toAbsolutePath().normalize()
        return try {
            val directoryAttributes = attributesOrNull(normalized) ?: return LatestReportLookup.Empty
            if (!directoryAttributes.isDirectory || directoryAttributes.isSymbolicLink) {
                return LatestReportLookup.Failure("Report output path is not a safe directory")
            }

            var latest: Candidate? = null
            var inspectedEntries = 0
            Files.newDirectoryStream(normalized).use { entries ->
                for (entry in entries) {
                    if (inspectedEntries >= MAX_INSPECTED_ENTRIES) {
                        return LatestReportLookup.Failure("Report directory contains too many entries to scan safely")
                    }
                    inspectedEntries++
                    val name = entry.fileName.toString()
                    val match = REPORT_NAME.matchEntire(name) ?: continue
                    val collision = if (match.groupValues[2].isEmpty()) 1
                    else match.groupValues[2].toIntOrNull()?.takeIf { it in 2..ReportNamingDefaults.MAX_COLLISION_ATTEMPTS } ?: continue
                    val attributes = attributesOrNull(entry) ?: continue
                    if (!attributes.isRegularFile || attributes.isSymbolicLink) continue
                    val candidate = Candidate(
                        entry.toAbsolutePath().normalize(),
                        attributes.lastModifiedTime(),
                        match.groupValues[1],
                        collision,
                        name,
                    )
                    if (latest == null || candidate > latest) latest = candidate
                }
            }
            latest?.let { LatestReportLookup.Found(it.path) } ?: LatestReportLookup.Empty
        } catch (exception: IOException) {
            LatestReportLookup.Failure("Report directory could not be scanned", exception)
        } catch (exception: DirectoryIteratorException) {
            LatestReportLookup.Failure("Report directory could not be scanned", exception.cause ?: exception)
        } catch (exception: SecurityException) {
            LatestReportLookup.Failure("Report directory could not be scanned", exception)
        }
    }

    private fun attributesOrNull(path: Path): BasicFileAttributes? = try {
        Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
    } catch (_: java.nio.file.NoSuchFileException) {
        null
    }

    private data class Candidate(
        val path: Path,
        val modified: FileTime,
        val timestamp: String,
        val collision: Int,
        val name: String,
    ) : Comparable<Candidate> {
        override fun compareTo(other: Candidate): Int {
            val timeComparison = modified.compareTo(other.modified)
            if (timeComparison != 0) return timeComparison
            val timestampComparison = timestamp.compareTo(other.timestamp)
            if (timestampComparison != 0) return timestampComparison
            val collisionComparison = collision.compareTo(other.collision)
            return if (collisionComparison != 0) collisionComparison else name.compareTo(other.name)
        }
    }
}

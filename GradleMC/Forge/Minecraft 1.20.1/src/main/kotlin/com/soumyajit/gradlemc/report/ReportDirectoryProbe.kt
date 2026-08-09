package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

sealed interface ReportDirectoryReadiness {
    data class Ready(val directory: Path, val alreadyExists: Boolean) : ReportDirectoryReadiness
    data class Failure(val directory: Path, val message: String, val cause: Throwable? = null) : ReportDirectoryReadiness
}

/** Read-only counterpart to [ReportFileAllocator]; it never creates an output directory. */
object ReportDirectoryProbe {
    fun inspect(gameDirectory: Path, config: GradleMCConfigSnapshot): ReportDirectoryReadiness {
        val game = gameDirectory.toAbsolutePath().normalize()
        val root = ReportPaths.gradleMcRoot(game)
        val reports = ReportPaths.reportDirectory(game, config)
        return try {
            val gameAttributes = attributes(game)
                ?: return ReportDirectoryReadiness.Failure(reports, "Game directory does not exist")
            if (!gameAttributes.isDirectory || gameAttributes.isSymbolicLink) {
                return ReportDirectoryReadiness.Failure(reports, "Game directory is not a safe directory")
            }

            val rootAttributes = attributes(root)
            if (rootAttributes == null) {
                return if (Files.isWritable(game)) ReportDirectoryReadiness.Ready(reports, false)
                else ReportDirectoryReadiness.Failure(reports, "Game directory is not writable")
            }
            if (!rootAttributes.isDirectory || rootAttributes.isSymbolicLink) {
                return ReportDirectoryReadiness.Failure(reports, "GradleMC output root is not a safe directory")
            }

            val reportAttributes = attributes(reports)
            if (reportAttributes == null) {
                return if (Files.isWritable(root)) ReportDirectoryReadiness.Ready(reports, false)
                else ReportDirectoryReadiness.Failure(reports, "GradleMC output root is not writable")
            }
            if (!reportAttributes.isDirectory || reportAttributes.isSymbolicLink) {
                return ReportDirectoryReadiness.Failure(reports, "Report output path is not a safe directory")
            }
            if (Files.isWritable(reports)) ReportDirectoryReadiness.Ready(reports, true)
            else ReportDirectoryReadiness.Failure(reports, "Report directory is not writable")
        } catch (exception: IOException) {
            ReportDirectoryReadiness.Failure(reports, "Report directory could not be inspected", exception)
        } catch (exception: SecurityException) {
            ReportDirectoryReadiness.Failure(reports, "Report directory could not be inspected", exception)
        }
    }

    private fun attributes(path: Path): BasicFileAttributes? = try {
        Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
    } catch (_: java.nio.file.NoSuchFileException) {
        null
    }
}

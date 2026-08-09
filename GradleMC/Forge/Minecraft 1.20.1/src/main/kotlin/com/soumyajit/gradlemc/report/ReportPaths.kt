package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import java.nio.file.Path

/** Lexical path calculation only; it never creates, scans, or writes directories. */
object ReportPaths {
    private const val OUTPUT_ROOT = "gradlemc"

    fun reportDirectory(gameDirectory: Path, config: GradleMCConfigSnapshot): Path {
        val root = gradleMcRoot(gameDirectory)
        return containedChild(root, config.reportDirectoryName)
    }

    fun gradleMcRoot(gameDirectory: Path): Path {
        require(gameDirectory.isAbsolute) { "gameDirectory must be absolute" }
        return gameDirectory.normalize().resolve(OUTPUT_ROOT).normalize()
    }

    fun resolveReportChild(reportDirectory: Path, segment: String): Path = containedChild(reportDirectory, segment)

    private fun containedChild(base: Path, segment: String): Path {
        require(base.isAbsolute) { "base directory must be absolute" }
        require(isSafeFilenameSegment(segment)) { "segment must be a safe single filename segment" }
        val normalizedBase = base.normalize()
        val resolved = normalizedBase.resolve(segment).normalize()
        require(resolved.startsWith(normalizedBase)) { "resolved path escapes its base directory" }
        return resolved
    }

    fun isSafeFilenameSegment(value: String): Boolean =
        value.isNotBlank() && value.length <= 128 && value != "." && value != ".." &&
            !value.endsWith(' ') && !value.endsWith('.') &&
            value.none { it.code < 32 || it in "\\/:*?\"<>|" }
}

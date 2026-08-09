package com.soumyajit.gradlemc.report

import java.nio.file.Path
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Deterministic report-name and candidate selection; actual file creation is a later slice. */
class ReportNameGenerator(
    private val clock: Clock,
    private val zoneId: ZoneId,
) {
    private val timestampFormatter = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss", Locale.ROOT).withZone(zoneId)

    fun fileName(
        prefix: String = ReportNamingDefaults.DEFAULT_PREFIX,
        extension: String = ReportNamingDefaults.DEFAULT_EXTENSION,
        collisionNumber: Int = 1,
    ): String {
        require(collisionNumber >= 1) { "collisionNumber must be at least one" }
        require(isSafePrefix(prefix)) { "prefix must be filename-safe" }
        val safeExtension = normalizedExtension(extension)
        require(!prefix.lowercase(Locale.ROOT).endsWith(safeExtension)) { "prefix must not include the extension" }
        val timestamp = timestampFormatter.format(clock.instant())
        val suffix = if (collisionNumber == 1) "" else "-$collisionNumber"
        val maximumPrefixLength = ReportNamingDefaults.MAX_FILENAME_LENGTH - timestamp.length - suffix.length - safeExtension.length
        require(maximumPrefixLength >= 1) { "filename components exceed the filename limit" }
        return prefix.take(maximumPrefixLength) + timestamp + suffix + safeExtension
    }

    fun selectAvailable(
        directory: Path,
        prefix: String = ReportNamingDefaults.DEFAULT_PREFIX,
        extension: String = ReportNamingDefaults.DEFAULT_EXTENSION,
        exists: (Path) -> Boolean,
    ): Path {
        val normalizedDirectory = directory.normalize()
        require(normalizedDirectory.isAbsolute) { "directory must be absolute" }
        repeat(ReportNamingDefaults.MAX_COLLISION_ATTEMPTS) { index ->
            val candidate = normalizedDirectory.resolve(fileName(prefix, extension, index + 1))
            if (!exists(candidate)) return candidate
        }
        throw IllegalStateException("No report filename is available within the bounded collision policy")
    }

    private fun normalizedExtension(extension: String): String {
        require(extension.startsWith('.') && extension.length in 2..16) { "extension must start with one dot" }
        require(extension.drop(1).all { it.isLetterOrDigit() }) { "extension must contain only letters or digits" }
        return extension.lowercase(Locale.ROOT)
    }

    private fun isSafePrefix(prefix: String): Boolean =
        prefix.isNotBlank() && !prefix.endsWith('.') && !prefix.endsWith(' ') &&
            prefix.none { it.code < 32 || it in "\\/:*?\"<>|" }
}

package com.soumyajit.gradlemc.report

import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern

/** Privacy-preserving presentation for paths and filesystem failures. */
object ReportPathDisplay {
    fun relativeToGame(gameDirectory: Path, path: Path): String {
        val game = gameDirectory.toAbsolutePath().normalize()
        val normalized = path.toAbsolutePath().normalize()
        return if (normalized.startsWith(game)) {
            game.relativize(normalized).toString().ifBlank { "." }
        } else {
            normalized.fileName?.toString() ?: "[external-path]"
        }
    }

    fun redact(value: String, gameDirectory: Path): String {
        var safe = replaceIgnoreCase(value, gameDirectory.toAbsolutePath().normalize().toString(), "[game-dir]")
        val home = System.getProperty("user.home", "").trim()
        if (home.isNotEmpty()) {
            val normalizedHome = runCatching { Path.of(home).toAbsolutePath().normalize().toString() }.getOrDefault(home)
            safe = replaceIgnoreCase(safe, normalizedHome, "[user-home]")
        }
        return safe
    }

    private fun replaceIgnoreCase(value: String, target: String, replacement: String): String {
        if (target.isBlank()) return value
        return Pattern.compile(Pattern.quote(target), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
            .matcher(value)
            .replaceAll(Matcher.quoteReplacement(replacement))
    }
}

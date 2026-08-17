package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMcConfigSnapshot
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ReportPaths {
    fun reportDirectory(gameDirectory: Path, config: GradleMcConfigSnapshot): Path {
        val game = gameDirectory.toAbsolutePath().normalize()
        val output = game.resolve("gradlemc").resolve(config.reportDirectoryName).normalize()
        require(output.startsWith(game))
        // Never follow an existing link between the game directory and GradleMC output.
        // This check is repeated by ReportFiles after directory creation before a file is written.
        var current = game
        for (part in game.relativize(output)) {
            current = current.resolve(part)
            require(!Files.isSymbolicLink(current)) { "GradleMC report output must not use symbolic links" }
        }
        return output
    }
}
object ReportFiles {
    private val stamp = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss").withZone(ZoneOffset.UTC)
    fun write(directory: Path, extension: String, content: String, now: Instant = Instant.now()): Path {
        require(extension in setOf("txt", "json"))
        Files.createDirectories(directory); require(Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(directory)) { "Report directory is unsafe" }
        for (i in 1..100) { val suffix = if (i == 1) "" else "-$i"; val target = directory.resolve("gradlemc-report-${stamp.format(now)}$suffix.$extension"); try { Files.newBufferedWriter(target, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW).use { it.write(content) }; return target } catch (_: java.nio.file.FileAlreadyExistsException) {} }
        error("Could not allocate a unique report name")
    }
    fun latest(directory: Path): Path? { if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) return null; return Files.list(directory).use { s -> s.filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(it) && it.fileName.toString().matches(Regex("gradlemc-report-\\d{8}-\\d{6}(-\\d+)?\\.(txt|json)")) }.limit(256).toList().maxWithOrNull(compareBy<Path> { Files.getLastModifiedTime(it).toMillis() }.thenBy { it.fileName.toString() }) } }
    /** Lists only GradleMC-owned report files.  This never traverses child directories. */
    fun list(directory: Path, limit: Int = 12): List<Path> {
        require(limit in 1..64) { "Report list limit must be between 1 and 64" }
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) return emptyList()
        return Files.list(directory).use { stream ->
            stream.filter { path ->
                Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path) &&
                    path.fileName.toString().matches(Regex("gradlemc-report-\\d{8}-\\d{6}(-\\d+)?\\.(txt|json)"))
            }.toList().sortedWith(compareByDescending<Path> { Files.getLastModifiedTime(it).toMillis() }.thenByDescending { it.fileName.toString() }).take(limit)
        }
    }
}

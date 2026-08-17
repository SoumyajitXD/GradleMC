package com.soumyajit.gradlemc.report

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Privacy-first support bundle: callers provide intentional generated text only; no logs or directories are traversed. */
object IssueBundleWriter {
    fun write(directory: Path, reportText: String, reportJson: String): Path {
        Files.createDirectories(directory)
        require(Files.isDirectory(directory) && !Files.isSymbolicLink(directory)) { "Issue bundle directory is unsafe" }
        val target = directory.resolve("gradlemc-issue-bundle.zip")
        val temp = Files.createTempFile(directory, ".gradlemc-bundle-", ".tmp")
        try {
            ZipOutputStream(Files.newOutputStream(temp, StandardOpenOption.TRUNCATE_EXISTING)).use { zip ->
                listOf("diagnostics.txt" to reportText, "diagnostics.json" to reportJson).forEach { (name, value) -> zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray(StandardCharsets.UTF_8)); zip.closeEntry() }
            }
            Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            return target
        } finally { Files.deleteIfExists(temp) }
    }
}

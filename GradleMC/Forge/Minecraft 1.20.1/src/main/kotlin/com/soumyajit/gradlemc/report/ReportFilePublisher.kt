package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE

/** Writes a complete temporary report and only then publishes its final filename. */
class ReportFilePublisher(
    private val nameGenerator: ReportNameGenerator,
    private val allocator: ReportFileAllocator = ReportFileAllocator(nameGenerator),
    private val writeTemporary: (Path, String) -> Unit = ::writeUtf8AndFlush,
) {
    fun publish(baseDirectory: Path, config: GradleMCConfigSnapshot, content: String): Path {
        val directory = allocator.prepareDirectory(baseDirectory, config)
        val temporary = try {
            Files.createTempFile(directory, ".gradlemc-report-write-", ".tmp")
        } catch (exception: IOException) {
            throw ReportAllocationException("Unable to create temporary report file", exception)
        }

        try {
            writeTemporary(temporary, content)
            repeat(ReportNamingDefaults.MAX_COLLISION_ATTEMPTS) { index ->
                val candidate = directory.resolve(nameGenerator.fileName(collisionNumber = index + 1))
                try {
                    publishWithoutReplacement(temporary, candidate)
                    return candidate.toAbsolutePath().normalize()
                } catch (_: FileAlreadyExistsException) {
                    // Preserve the complete temporary file and try the next bounded final name.
                }
            }
            throw ReportCollisionExhaustedException("No report filename is available within the bounded collision policy")
        } catch (exception: ReportFileAllocationException) {
            throw exception
        } catch (exception: IOException) {
            throw ReportAllocationException("Unable to publish report file", exception)
        } finally {
            try {
                Files.deleteIfExists(temporary)
            } catch (_: IOException) {
                // A failed best-effort cleanup never changes a successful final publication.
            }
        }
    }

    private fun publishWithoutReplacement(source: Path, target: Path) {
        try {
            Files.createLink(target, source)
        } catch (exception: FileAlreadyExistsException) {
            throw exception
        } catch (_: UnsupportedOperationException) {
            Files.move(source, target)
        } catch (_: IOException) {
            // Same-directory move remains an exclusive fallback when hard links are unavailable.
            Files.move(source, target)
        }
    }

    companion object {
        private fun writeUtf8AndFlush(path: Path, content: String) {
            val encoded = StandardCharsets.UTF_8.encode(content)
            java.nio.channels.FileChannel.open(path, WRITE, TRUNCATE_EXISTING).use { channel ->
                while (encoded.hasRemaining()) channel.write(encoded)
                channel.force(true)
            }
        }
    }
}

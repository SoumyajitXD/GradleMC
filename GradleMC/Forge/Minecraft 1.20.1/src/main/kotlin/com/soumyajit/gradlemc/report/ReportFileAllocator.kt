package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import java.io.IOException
import java.nio.channels.Channels
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.SecureDirectoryStream
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE

/**
 * Prepares GradleMC's report directory beneath an explicit trusted base and reserves one empty report file.
 *
 * The portable fallback cannot prevent a hostile actor from replacing parent directories between NIO calls.
 * Candidate creation itself is always exclusive, so an existing candidate is never overwritten.
 */
class ReportFileAllocator(
    private val nameGenerator: ReportNameGenerator,
) {
    enum class AllocationStrategy { SECURE_DIRECTORY_STREAM, PORTABLE_FALLBACK }

    var lastAllocationStrategy: AllocationStrategy? = null
        private set

    @Throws(ReportFileAllocationException::class)
    fun reserve(
        baseDirectory: Path,
        config: GradleMCConfigSnapshot,
        prefix: String = ReportNamingDefaults.DEFAULT_PREFIX,
        extension: String = ReportNamingDefaults.DEFAULT_EXTENSION,
    ): Path {
        val trustedBase = trustedBase(baseDirectory)
        val reportDirectory = prepareReportDirectory(trustedBase, config)

        Files.newDirectoryStream(reportDirectory).use { stream ->
            if (stream is SecureDirectoryStream<Path>) {
                lastAllocationStrategy = AllocationStrategy.SECURE_DIRECTORY_STREAM
                return reserveSecurely(stream, reportDirectory, trustedBase, prefix, extension)
            }
        }
        lastAllocationStrategy = AllocationStrategy.PORTABLE_FALLBACK
        return reservePortably(reportDirectory, trustedBase, prefix, extension)
    }

    /** Prepares and resolves the report directory without reserving a final filename. */
    fun prepareDirectory(baseDirectory: Path, config: GradleMCConfigSnapshot): Path {
        val trustedBase = trustedBase(baseDirectory)
        return prepareReportDirectory(trustedBase, config)
    }

    private fun prepareReportDirectory(trustedBase: Path, config: GradleMCConfigSnapshot): Path {
        val gradleMcDirectory = prepareDirectChild(trustedBase, "gradlemc", trustedBase)
        return prepareDirectChild(gradleMcDirectory, config.reportDirectoryName, trustedBase)
    }

    private fun trustedBase(baseDirectory: Path): Path {
        val absolute = baseDirectory.toAbsolutePath().normalize()
        val attributes = try {
            Files.readAttributes(absolute, java.nio.file.attribute.BasicFileAttributes::class.java, NOFOLLOW_LINKS)
        } catch (exception: IOException) {
            throw InvalidReportBaseException("Report base does not exist or cannot be inspected", exception)
        }
        if (!attributes.isDirectory || attributes.isSymbolicLink) {
            throw InvalidReportBaseException("Report base must be an existing non-symbolic-link directory")
        }
        return try {
            absolute.toRealPath()
        } catch (exception: IOException) {
            throw InvalidReportBaseException("Report base cannot be resolved", exception)
        }
    }

    private fun prepareDirectChild(parent: Path, segment: String, trustedBase: Path): Path {
        require(ReportPaths.isSafeFilenameSegment(segment)) { "directory segment must be safe" }
        val child = parent.resolve(segment).normalize()
        if (!child.startsWith(parent)) throw ReportPathEscapeException("Report directory escapes its parent")

        val attributes = attributesOrNull(child)
        if (attributes == null) {
            try {
                Files.createDirectory(child)
            } catch (_: FileAlreadyExistsException) {
                // Another creator won; inspect the object it created below.
            } catch (exception: IOException) {
                throw ReportDirectoryCreationException("Unable to create report directory", exception)
            }
        }
        val inspected = attributesOrNull(child)
            ?: throw ReportDirectoryCreationException("Report directory was not created")
        if (inspected.isSymbolicLink) throw UnsafeReportDirectoryException("Report directory must not be a symbolic link")
        if (!inspected.isDirectory) throw UnsafeReportDirectoryException("Report directory must be a directory")

        val realChild = try {
            child.toRealPath()
        } catch (exception: IOException) {
            throw UnsafeReportDirectoryException("Report directory cannot be resolved", exception)
        }
        if (!realChild.startsWith(parent) || !realChild.startsWith(trustedBase)) {
            throw ReportPathEscapeException("Report directory resolves outside the trusted base")
        }
        return realChild
    }

    private fun reserveSecurely(
        directory: SecureDirectoryStream<Path>,
        reportDirectory: Path,
        trustedBase: Path,
        prefix: String,
        extension: String,
    ): Path {
        repeat(ReportNamingDefaults.MAX_COLLISION_ATTEMPTS) { index ->
            val name = nameGenerator.fileName(prefix, extension, index + 1)
            try {
                directory.newByteChannel(Path.of(name), setOf(CREATE_NEW, WRITE)).use { channel ->
                    Channels.newOutputStream(channel).flush()
                }
                return verifyReservation(reportDirectory.resolve(name), reportDirectory, trustedBase)
            } catch (_: FileAlreadyExistsException) {
                // An existing file or link occupies this candidate; try the bounded next candidate.
            } catch (exception: IOException) {
                throw ReportAllocationException("Unable to reserve report file", exception)
            }
        }
        throw ReportCollisionExhaustedException("No report filename is available within the bounded collision policy")
    }

    private fun reservePortably(reportDirectory: Path, trustedBase: Path, prefix: String, extension: String): Path {
        repeat(ReportNamingDefaults.MAX_COLLISION_ATTEMPTS) { index ->
            val candidate = reportDirectory.resolve(nameGenerator.fileName(prefix, extension, index + 1))
            try {
                Files.newByteChannel(candidate, setOf(CREATE_NEW, WRITE)).use { }
                return verifyReservation(candidate, reportDirectory, trustedBase)
            } catch (_: FileAlreadyExistsException) {
                // Atomic exclusive creation treats existing links and files alike as occupied.
            } catch (exception: IOException) {
                throw ReportAllocationException("Unable to reserve report file", exception)
            }
        }
        throw ReportCollisionExhaustedException("No report filename is available within the bounded collision policy")
    }

    private fun verifyReservation(candidate: Path, reportDirectory: Path, trustedBase: Path): Path {
        val realCandidate = try {
            candidate.toRealPath()
        } catch (exception: IOException) {
            throw ReportAllocationException("Reserved report file cannot be resolved", exception)
        }
        if (!realCandidate.startsWith(reportDirectory) || !realCandidate.startsWith(trustedBase)) {
            throw ReportPathEscapeException("Reserved report file resolves outside the trusted base")
        }
        return realCandidate
    }

    private fun attributesOrNull(path: Path): java.nio.file.attribute.BasicFileAttributes? =
        try {
            Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes::class.java, NOFOLLOW_LINKS)
        } catch (_: java.nio.file.NoSuchFileException) {
            null
        } catch (exception: IOException) {
            throw UnsafeReportDirectoryException("Unable to inspect report directory", exception)
        }
}

sealed class ReportFileAllocationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidReportBaseException(message: String, cause: Throwable? = null) : ReportFileAllocationException(message, cause)
class UnsafeReportDirectoryException(message: String, cause: Throwable? = null) : ReportFileAllocationException(message, cause)
class ReportPathEscapeException(message: String, cause: Throwable? = null) : ReportFileAllocationException(message, cause)
class ReportDirectoryCreationException(message: String, cause: Throwable? = null) : ReportFileAllocationException(message, cause)
class ReportAllocationException(message: String, cause: Throwable? = null) : ReportFileAllocationException(message, cause)
class ReportCollisionExhaustedException(message: String) : ReportFileAllocationException(message)

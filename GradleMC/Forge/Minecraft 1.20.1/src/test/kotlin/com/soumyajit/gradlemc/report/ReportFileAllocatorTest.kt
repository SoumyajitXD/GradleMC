package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.opentest4j.TestAbortedException
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReportFileAllocatorTest {
    private val roots = mutableListOf<Path>()
    private val clock = Clock.fixed(Instant.parse("2026-07-31T15:17:00Z"), ZoneOffset.UTC)
    private val config = GradleMCConfigSnapshot.defaults()

    @AfterTest
    fun cleanUp() {
        roots.forEach { root ->
            if (Files.exists(root)) Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }

    @Test
    fun `reserve prepares direct directories and creates an empty first candidate`() {
        val base = tempBase()
        val result = allocator().reserve(base, config)

        assertEquals(base.toRealPath().resolve("gradlemc").resolve("reports"), result.parent)
        assertEquals("gradlemc-report-20260731-151700.txt", result.fileName.toString())
        assertEquals(0, Files.size(result))
        assertTrue(Files.isRegularFile(result))
        assertTrue(result.toRealPath().startsWith(base.toRealPath()))
    }

    @Test
    fun `missing or file bases fail without writes`() {
        val parent = tempBase()
        val missing = parent.resolve("missing")
        assertFailsWith<InvalidReportBaseException> { allocator().reserve(missing, config) }
        assertFalse(Files.exists(missing.resolve("gradlemc")))
        val file = parent.resolve("base-file").also { it.writeText("sentinel") }
        assertFailsWith<InvalidReportBaseException> { allocator().reserve(file, config) }
        assertEquals("sentinel", file.readText())
    }

    @Test
    fun `existing files where directories are required are rejected`() {
        val base = tempBase()
        base.resolve("gradlemc").writeText("sentinel")
        assertFailsWith<UnsafeReportDirectoryException> { allocator().reserve(base, config) }
        assertEquals("sentinel", base.resolve("gradlemc").readText())
    }

    @Test
    fun `occupied candidates are never modified and allocation is bounded`() {
        val base = tempBase()
        val directory = base.resolve("gradlemc").resolve("reports")
        Files.createDirectories(directory)
        val generator = ReportNameGenerator(clock, ZoneOffset.UTC)
        directory.resolve(generator.fileName()).writeText("first")
        val result = allocator().reserve(base, config)
        assertEquals(generator.fileName(collisionNumber = 2), result.fileName.toString())
        assertEquals("first", directory.resolve(generator.fileName()).readText())

        (2..ReportNamingDefaults.MAX_COLLISION_ATTEMPTS).forEach { number ->
            directory.resolve(generator.fileName(collisionNumber = number)).writeText("$number")
        }
        assertFailsWith<ReportCollisionExhaustedException> { allocator().reserve(base, config) }
        assertEquals("first", directory.resolve(generator.fileName()).readText())
        assertEquals("2", directory.resolve(generator.fileName(collisionNumber = 2)).readText())
    }

    @Test
    fun `concurrent reservations are unique and leave sentinels untouched`() {
        val base = tempBase()
        val sentinel = base.resolve("sentinel.txt").also { it.writeText("unchanged") }
        val workers = 8
        val executor = Executors.newFixedThreadPool(workers)
        val start = CountDownLatch(1)
        try {
            val futures = (1..workers).map {
                executor.submit<Path> {
                    start.await(5, TimeUnit.SECONDS)
                    allocator().reserve(base, config)
                }
            }
            start.countDown()
            val results = futures.map { it.get(10, TimeUnit.SECONDS) }
            assertEquals(workers, results.toSet().size)
            results.forEach { path ->
                assertTrue(Files.exists(path))
                assertEquals(0, Files.size(path))
                assertTrue(path.startsWith(base.toRealPath()))
            }
            assertEquals("unchanged", sentinel.readText())
        } finally {
            executor.shutdown()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `report directory symlink is rejected when supported`() {
        val base = tempBase()
        val outside = tempBase()
        val root = base.resolve("gradlemc").also(Files::createDirectory)
        val link = root.resolve("reports")
        try {
            Files.createSymbolicLink(link, outside)
        } catch (exception: UnsupportedOperationException) {
            throw TestAbortedException("Symbolic-link creation is unsupported by this filesystem")
        } catch (exception: java.nio.file.FileSystemException) {
            throw TestAbortedException("Symbolic-link creation is denied by the test environment", exception)
        }
        assertTrue(Files.readAttributes(link, BasicFileAttributes::class.java, java.nio.file.LinkOption.NOFOLLOW_LINKS).isSymbolicLink)
        assertFailsWith<UnsafeReportDirectoryException> { allocator().reserve(base, config) }
        assertTrue(Files.exists(outside))
    }

    @Test
    fun `candidate symlink is occupied and its target stays unchanged`() {
        val base = tempBase()
        val target = base.resolve("target.txt").also { it.writeText("sentinel") }
        val directory = base.resolve("gradlemc").resolve("reports")
        Files.createDirectories(directory)
        val generator = ReportNameGenerator(clock, ZoneOffset.UTC)
        try {
            Files.createSymbolicLink(directory.resolve(generator.fileName()), target)
        } catch (exception: UnsupportedOperationException) {
            throw TestAbortedException("Symbolic-link creation is unsupported by this filesystem")
        } catch (exception: java.nio.file.FileSystemException) {
            throw TestAbortedException("Symbolic-link creation is denied by the test environment", exception)
        }
        val result = allocator().reserve(base, config)
        assertEquals(generator.fileName(collisionNumber = 2), result.fileName.toString())
        assertEquals("sentinel", target.readText())
    }

    @Test
    fun `allocator reports the provider path used`() {
        val allocator = allocator()
        allocator.reserve(tempBase(), config)
        assertNotNull(allocator.lastAllocationStrategy)
    }

    private fun allocator() = ReportFileAllocator(ReportNameGenerator(clock, ZoneOffset.UTC))

    private fun tempBase(): Path = Files.createTempDirectory("gradlemc-allocation-").also(roots::add)
}

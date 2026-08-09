package com.soumyajit.gradlemc.report

import com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
import java.io.IOException
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ReportFilePublisherTest {
    private val clock = Clock.fixed(Instant.parse("2026-08-02T01:02:03Z"), ZoneOffset.UTC)
    private val config = GradleMCConfigSnapshot.defaults()

    @Test
    fun `complete UTF-8 content is published without temporary residue`() {
        val game = Files.createTempDirectory("gradlemc-publish-")
        val published = publisher().publish(game, config, "diagnostic résumé\n")

        assertEquals("diagnostic résumé\n", published.readText())
        assertEquals("gradlemc-report-20260802-010203.txt", published.fileName.toString())
        assertFalse(Files.list(published.parent).use { paths -> paths.anyMatch { it.fileName.toString().endsWith(".tmp") } })
    }

    @Test
    fun `occupied final report is preserved and next collision is published`() {
        val game = Files.createTempDirectory("gradlemc-publish-")
        val reports = game.resolve("gradlemc").resolve("reports")
        Files.createDirectories(reports)
        val first = reports.resolve("gradlemc-report-20260802-010203.txt").also { it.writeText("sentinel") }

        val published = publisher().publish(game, config, "complete")
        assertEquals("sentinel", first.readText())
        assertEquals("gradlemc-report-20260802-010203-2.txt", published.fileName.toString())
        assertEquals("complete", published.readText())
    }

    @Test
    fun `exhausted final names preserve occupants and remove temporary output`() {
        val game = Files.createTempDirectory("gradlemc-publish-")
        val reports = game.resolve("gradlemc").resolve("reports")
        Files.createDirectories(reports)
        val generator = ReportNameGenerator(clock, ZoneOffset.UTC)
        val occupied = (1..ReportNamingDefaults.MAX_COLLISION_ATTEMPTS).associateWith { collisionNumber ->
            reports.resolve(generator.fileName(collisionNumber = collisionNumber)).also {
                it.writeText("sentinel-$collisionNumber")
            }
        }

        assertFailsWith<ReportCollisionExhaustedException> { publisher().publish(game, config, "complete") }
        occupied.forEach { (collisionNumber, path) ->
            assertEquals("sentinel-$collisionNumber", path.readText())
        }
        assertFalse(Files.list(reports).use { paths -> paths.anyMatch { it.fileName.toString().endsWith(".tmp") } })
    }

    @Test
    fun `write failure leaves no final or partial report`() {
        val game = Files.createTempDirectory("gradlemc-publish-")
        val failing = ReportFilePublisher(ReportNameGenerator(clock, ZoneOffset.UTC), writeTemporary = { path, _ ->
            path.writeText("partial")
            throw IOException("injected write failure")
        })

        assertFailsWith<ReportAllocationException> { failing.publish(game, config, "complete") }
        val reports = game.resolve("gradlemc").resolve("reports")
        assertFalse(Files.newDirectoryStream(reports).use { it.iterator().hasNext() })
    }

    private fun publisher() = ReportFilePublisher(ReportNameGenerator(clock, ZoneOffset.UTC))
}

package com.soumyajit.gradlemc.report

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals

class IssueBundleWriterTest { @TempDir lateinit var temp:Path
 @Test fun `bundle has only intentional diagnostic allowlist`() { val file=IssueBundleWriter.write(temp,"report","{} ");ZipFile(file.toFile()).use { assertEquals(setOf("diagnostics.txt","diagnostics.json"),it.entries().asSequence().map { e->e.name }.toSet()) } }
}

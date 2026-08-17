package com.soumyajit.gradlemc.config

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GradleMcConfigTest {
 @TempDir lateinit var temp:Path
 @Test fun `malformed properties recover per value`() { val f=temp.resolve("gradlemc.properties");Files.writeString(f,"reportDirectoryName=CON\nkeyBindingEnabled=maybe\n");assertEquals(GradleMcConfigSnapshot(),GradleMcConfig.load(f)) }
 @Test fun `save load round trip and windows unsafe names are rejected`() { val f=temp.resolve("x.properties");GradleMcConfig.save(GradleMcConfigSnapshot("reports-2",false),f);assertEquals(GradleMcConfigSnapshot("reports-2",false),GradleMcConfig.load(f));assertNull(GradleMcConfigSnapshot.safeDirectory("foo/bar"));assertNull(GradleMcConfigSnapshot.safeDirectory("LPT1")) }
 @Test fun `overlay choices persist and unsupported windows recover safely`() {
  val f=temp.resolve("overlay.properties")
  val enabled=GradleMcConfigSnapshot(overlayEnabled=true,overlayShowTitle=true,overlayShowFps=true,overlayShowAverageFps=true,overlaySamplingWindowSeconds=120)
  GradleMcConfig.save(enabled,f)
  assertEquals(enabled,GradleMcConfig.load(f))
  Files.writeString(f,"overlayEnabled=true\nshowOverlayTitle=true\noverlayShowFps=false\noverlayShowAverageFps=true\noverlaySamplingWindowSeconds=45\n")
  val recovered=GradleMcConfig.load(f)
  assertEquals(60,recovered.overlaySamplingWindowSeconds)
  assertEquals(true,recovered.overlayEnabled)
  assertEquals(true,recovered.overlayShowTitle)
  assertEquals(false,recovered.overlayShowFps)
  assertEquals(true,recovered.overlayShowAverageFps)
 }
}

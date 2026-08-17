package com.soumyajit.gradlemc.config

import net.fabricmc.loader.api.FabricLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

data class GradleMcConfigSnapshot(val reportDirectoryName: String = "reports", val keyBindingEnabled: Boolean = true, val overlayEnabled: Boolean = false, val overlayShowTitle: Boolean = false, val overlayShowFps: Boolean = true, val overlayShowAverageFps: Boolean = false, val overlaySamplingWindowSeconds: Int = 60, val performanceMode: String = "balanced") {
    companion object {
        private val reserved = setOf("CON", "PRN", "AUX", "NUL") + (1..9).flatMap { listOf("COM$it", "LPT$it") }
        fun safeDirectory(value: String?): String? = value?.trim()?.takeIf {
            it.length in 1..64 && it.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) &&
                it !in setOf(".", "..") && !it.endsWith('.') && !it.endsWith(' ') && it.uppercase() !in reserved
        }
        fun window(value: String?) = value?.toIntOrNull()?.takeIf { it in setOf(30, 60, 120) } ?: 60
        fun mode(value: String?) = value?.trim()?.lowercase()?.takeIf { it in setOf("low_impact", "balanced", "detailed") } ?: "balanced"
    }
}

/** Small, owned configuration; malformed values fall back individually and never delete a good file. */
object GradleMcConfig {
    @Volatile private var snapshot = GradleMcConfigSnapshot()
    val file: Path get() = FabricLoader.getInstance().gameDir.resolve("gradlemc").resolve("gradlemc.properties")
    fun load(): GradleMcConfigSnapshot = load(file).also { snapshot = it }
    fun load(path: Path): GradleMcConfigSnapshot {
        val p = Properties(); runCatching { if (Files.isRegularFile(path)) Files.newInputStream(path).use(p::load) }
        return GradleMcConfigSnapshot(GradleMcConfigSnapshot.safeDirectory(p.getProperty("reportDirectoryName")) ?: "reports", p.getProperty("keyBindingEnabled", "true").toBooleanStrictOrNull() ?: true, p.getProperty("overlayEnabled", "false").toBooleanStrictOrNull() ?: false, p.getProperty("showOverlayTitle", "false").toBooleanStrictOrNull() ?: false, p.getProperty("overlayShowFps", "true").toBooleanStrictOrNull() ?: true, p.getProperty("overlayShowAverageFps", "false").toBooleanStrictOrNull() ?: false, GradleMcConfigSnapshot.window(p.getProperty("overlaySamplingWindowSeconds")), GradleMcConfigSnapshot.mode(p.getProperty("performanceMode")))
    }
    fun save(value: GradleMcConfigSnapshot = snapshot, path: Path = file) {
        val safe = value.copy(reportDirectoryName = GradleMcConfigSnapshot.safeDirectory(value.reportDirectoryName) ?: "reports", overlaySamplingWindowSeconds = GradleMcConfigSnapshot.window(value.overlaySamplingWindowSeconds.toString()), performanceMode = GradleMcConfigSnapshot.mode(value.performanceMode))
        Files.createDirectories(path.parent); val temp = Files.createTempFile(path.parent, ".gradlemc-", ".tmp")
        try {
            Files.newBufferedWriter(temp, StandardCharsets.UTF_8).use { it.write("reportDirectoryName=${safe.reportDirectoryName}\nkeyBindingEnabled=${safe.keyBindingEnabled}\noverlayEnabled=${safe.overlayEnabled}\nshowOverlayTitle=${safe.overlayShowTitle}\noverlayShowFps=${safe.overlayShowFps}\noverlayShowAverageFps=${safe.overlayShowAverageFps}\noverlaySamplingWindowSeconds=${safe.overlaySamplingWindowSeconds}\nperformanceMode=${safe.performanceMode}\n") }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                // Some game directories are mounted on filesystems without atomic rename.
                // The same-directory temporary file still keeps the fallback narrow.
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        finally { Files.deleteIfExists(temp) }; snapshot = safe
    }
    fun current(): GradleMcConfigSnapshot = snapshot
    @Synchronized fun update(transform: (GradleMcConfigSnapshot) -> GradleMcConfigSnapshot) { save(transform(snapshot)) }
}

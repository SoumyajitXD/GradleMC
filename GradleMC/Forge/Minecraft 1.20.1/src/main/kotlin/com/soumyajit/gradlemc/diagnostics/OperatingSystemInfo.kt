package com.soumyajit.gradlemc.diagnostics

import java.util.concurrent.TimeUnit

/** Immutable OS presentation shared by diagnostics, reports, and client screens. */
data class OperatingSystemInfo(
    val displayName: String,
    val windowsBuild: Int? = null,
)

/**
 * Pure classification boundary. Windows 11 keeps the NT 10.0 version; its marketing name is
 * determined from the Windows build number, not the compatibility-oriented JVM OS name.
 */
internal object OperatingSystemClassifier {
    private const val WINDOWS_11_FIRST_BUILD = 22_000

    fun classify(osName: String?, osVersion: String?, rawWindowsBuild: String?): OperatingSystemInfo {
        val name = osName.orEmpty().trim().ifBlank { "unknown" }
        val version = osVersion.orEmpty().trim()
        if (!name.equals("Windows", ignoreCase = true) && !name.startsWith("Windows ", ignoreCase = true)) {
            return OperatingSystemInfo(listOf(name, version).filter { it.isNotBlank() }.joinToString(" "))
        }

        val build = rawWindowsBuild?.trim()?.toIntOrNull()?.takeIf { it >= 0 }
        if (build == null) {
            return OperatingSystemInfo(listOf(name, version).filter { it.isNotBlank() }.joinToString(" "))
        }

        val marketingName = if (build >= WINDOWS_11_FIRST_BUILD) "Windows 11" else "Windows 10"
        val detailedVersion = when {
            version.isBlank() -> "build $build"
            version.endsWith(".$build") -> version
            else -> "$version.$build"
        }
        return OperatingSystemInfo("$marketingName $detailedVersion", build)
    }
}

/**
 * Reads immutable OS information once during mod bootstrap. The registry query is Windows-only,
 * bounded, and never occurs on GUI rendering or refresh paths.
 */
object OperatingSystemInfoProvider {
    private val detected: OperatingSystemInfo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val name = System.getProperty("os.name")
        val version = System.getProperty("os.version")
        val build = if (name.orEmpty().startsWith("Windows", ignoreCase = true)) WindowsRegistryBuildReader.read() else null
        OperatingSystemClassifier.classify(name, version, build)
    }

    fun initialize() {
        detected
    }

    fun current(): OperatingSystemInfo = detected
}

private object WindowsRegistryBuildReader {
    private const val TIMEOUT_SECONDS = 2L
    private val buildLine = Regex("""(?im)^\s*CurrentBuildNumber\s+REG_\S+\s+(\d+)\s*$""")

    fun read(): String? = runCatching {
        val process = ProcessBuilder(
            "reg.exe", "query", "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "/v", "CurrentBuildNumber",
        ).redirectErrorStream(true).start()
        try {
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                return@runCatching null
            }
            if (process.exitValue() != 0) return@runCatching null
            buildLine.find(process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })?.groupValues?.get(1)
        } finally {
            process.destroy()
        }
    }.getOrNull()
}

package com.soumyajit.gradlemc.diagnostics

import java.util.concurrent.TimeUnit

data class OperatingSystemInfo(val displayName: String, val windowsBuild: Int? = null)

/** Windows 11 retains NT 10.0; only its build number identifies its marketing version. */
internal object OperatingSystemClassifier {
    fun classify(osName: String?, osVersion: String?, rawWindowsBuild: String?): OperatingSystemInfo {
        val name = osName.orEmpty().trim().ifBlank { "unknown" }
        val version = osVersion.orEmpty().trim()
        if (!name.equals("Windows", true) && !name.startsWith("Windows ", true)) {
            return OperatingSystemInfo(listOf(name, version).filter(String::isNotBlank).joinToString(" "))
        }
        val build = rawWindowsBuild?.trim()?.toIntOrNull()?.takeIf { it >= 0 }
            ?: return OperatingSystemInfo(listOf(name, version).filter(String::isNotBlank).joinToString(" "))
        val marketingName = if (build >= 22_000) "Windows 11" else "Windows 10"
        val detail = if (version.isBlank() || version.endsWith(".$build")) version.ifBlank { "build $build" } else "$version.$build"
        return OperatingSystemInfo("$marketingName $detail", build)
    }
}

object OperatingSystemInfoProvider {
    private val detected by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val name = System.getProperty("os.name")
        OperatingSystemClassifier.classify(name, System.getProperty("os.version"), if (name.orEmpty().startsWith("Windows", true)) WindowsBuildReader.read() else null)
    }
    fun initialize() { detected }
    fun current(): OperatingSystemInfo = detected
}

private object WindowsBuildReader {
    private val buildLine = Regex("""(?im)^\s*CurrentBuildNumber\s+REG_\S+\s+(\d+)\s*$""")
    fun read(): String? = runCatching {
        val process = ProcessBuilder("reg.exe", "query", "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "/v", "CurrentBuildNumber").redirectErrorStream(true).start()
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) null
            else buildLine.find(process.inputStream.bufferedReader().use { it.readText() })?.groupValues?.get(1)
        } finally { process.destroy() }
    }.getOrNull()
}

package com.soumyajit.gradlemc.diagnostics

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModEnvironment

data class ModSummary(val id: String, val name: String, val version: String, val environment: String)

/** Bounded, local metadata view; it deliberately does not inspect arbitrary JAR contents. */
object ModTools {
    fun all(): List<ModSummary> = FabricLoader.getInstance().allMods.map { container ->
        val metadata = container.metadata
        ModSummary(metadata.id, metadata.name.ifBlank { metadata.id }, metadata.version.friendlyString, metadata.environment.name.lowercase())
    }.sortedBy { it.id }

    fun find(id: String): ModSummary? = all().firstOrNull { it.id.equals(id, ignoreCase = true) }

    fun audit(): List<String> {
        val mods = all()
        val messages = buildList {
            val duplicateIds = mods.groupBy { it.id }.filterValues { it.size > 1 }.keys
            if (duplicateIds.isNotEmpty()) add("Duplicate metadata IDs: ${duplicateIds.joinToString(", ")}")
            val ids = mods.map { it.id }.toSet()
            if ("fabric-api" !in ids) add("Fabric API is not present.")
            if ("fabric-language-kotlin" !in ids) add("Fabric Language Kotlin is not present.")
        }
        return messages.ifEmpty { listOf("No deterministic GradleMC mod-metadata problems were found.") }
    }
}

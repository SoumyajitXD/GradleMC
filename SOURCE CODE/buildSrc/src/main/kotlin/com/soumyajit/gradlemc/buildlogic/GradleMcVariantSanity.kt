package com.soumyajit.gradlemc.buildlogic

object GradleMcVariantSanity {
    private val artifactPattern = Regex("""^gradlemc-[A-Za-z0-9_.+-]+-(forge|fabric|neoforge)-\d+(?:\.\d+){1,2}\.jar$""")

    @JvmStatic
    fun validateVariantArtifacts(matrix: Map<*, *>): List<String> {
        val errors = mutableListOf<String>()
        val modVersion = matrix["modVersion"] as? String
        val variants = matrix["variants"] as? List<*> ?: return listOf("root.variants must be a list")

        for (rawVariant in variants) {
            val variant = rawVariant as? Map<*, *> ?: continue
            val id = variant["id"] as? String ?: "<missing-id>"
            val loader = variant["loader"] as? String
            val minecraftVersion = variant["minecraftVersion"] as? String
            val artifactName = variant["artifactName"] as? String
            val buildable = variant["buildable"] as? Boolean ?: false
            val publish = variant["publish"] as? Boolean ?: false

            if (artifactName.isNullOrBlank()) {
                errors += "$id: artifactName is required"
                continue
            }
            if (!artifactPattern.matches(artifactName)) {
                errors += "$id: artifactName must look like gradlemc-<version>-<loader>-<minecraft>.jar"
            }
            if (modVersion != null && !artifactName.startsWith("gradlemc-$modVersion-")) {
                errors += "$id: artifactName must include modVersion $modVersion"
            }
            if (loader != null && "-$loader-" !in artifactName) {
                errors += "$id: artifactName must include loader $loader"
            }
            if (minecraftVersion != null && !artifactName.endsWith("-$minecraftVersion.jar")) {
                errors += "$id: artifactName must end with minecraftVersion $minecraftVersion.jar"
            }
            if ((buildable || publish) && artifactName.contains("SNAPSHOT", ignoreCase = true)) {
                errors += "$id: buildable/publishable artifactName must not be a snapshot placeholder"
            }
        }

        return errors
    }

    @JvmStatic
    fun variantSummary(matrix: Map<*, *>): String {
        val variants = matrix["variants"] as? List<*> ?: emptyList<Any>()
        val rows = variants.mapNotNull { raw ->
            val variant = raw as? Map<*, *> ?: return@mapNotNull null
            val id = variant["id"] as? String ?: return@mapNotNull null
            val status = variant["status"] as? String ?: "unknown"
            val buildable = variant["buildable"] as? Boolean ?: false
            "$id [$status, buildable=$buildable]"
        }
        return rows.joinToString(System.lineSeparator())
    }
}

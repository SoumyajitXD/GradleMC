package com.soumyajit.gradlemc.modaudit;

import java.util.List;
import java.util.Locale;

/** Immutable, report-safe description of one actively loaded mod. */
public record ModDescriptor(String modId, String namespace, String displayName, String version,
                            String jarFileName, int entriesInOwningFile,
                            List<ModDependencyDescriptor> dependencies, boolean hasUpdateUrl,
                            boolean hasModPageUrl, List<String> metadataObservations) {
    public ModDescriptor {
        modId = normalize(modId);
        namespace = normalize(namespace);
        displayName = displayName == null ? "" : displayName.trim();
        version = version == null ? "" : version.trim();
        jarFileName = safeFileName(jarFileName);
        entriesInOwningFile = Math.max(0, entriesInOwningFile);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        metadataObservations = metadataObservations == null ? List.of() : List.copyOf(metadataObservations);
    }

    public String normalizedIdentifier() { return modId + "@" + version; }
    public static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static String safeFileName(String value) {
        if (value == null || value.isBlank()) return "";
        try { return java.nio.file.Path.of(value).getFileName().toString(); }
        catch (RuntimeException ignored) { return ""; }
    }
}

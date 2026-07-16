package com.soumyajit.gradlemc.modaudit;

import com.google.gson.GsonBuilder;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.report.ReportFileNames;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import net.minecraft.SharedConstants;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.versions.forge.ForgeVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModAuditReportWriter {
    public record WrittenReports(Path text, Path json) { }
    public WrittenReports write(ModAuditResult audit) throws IOException {
        Path directory = GradleMcPaths.exportDirectory(); Files.createDirectories(directory);
        Path text = ReportFileNames.unique(directory, "gradlemc-mod-audit-", audit.completedAt(), ".txt");
        Path json = text.resolveSibling(text.getFileName().toString().replaceFirst("\\.txt$", ".json"));
        Files.write(text, textLines(audit), StandardCharsets.UTF_8);
        Map<String, Object> report = new java.util.TreeMap<>();
        report.put("schemaVersion", 1); report.put("gradleMcVersion", GradleMCVersion.version()); report.put("minecraft", SharedConstants.getCurrentVersion().getName()); report.put("forge", ForgeVersion.getVersion()); report.put("java", System.getProperty("java.version", "unknown")); report.put("environmentSide", FMLEnvironment.dist.name()); report.put("generatedAt", audit.completedAt().toString());
        report.put("summary", Map.of("loadedMods", audit.snapshot().mods().size(), "owningModFiles", audit.snapshot().owningFileCount(), "findings", audit.findings().size()));
        report.put("loadedMods", reportMods(audit.snapshot().mods())); report.put("dependencyGraph", audit.dependencyGraph().reverse()); report.put("auditFindings", audit.findings());
        report.put("limitations", List.of("Local Forge metadata only; no network requests, jar scans, or telemetry.", "Runtime presence and dependency structure do not prove a mod caused lag."));
        Files.writeString(json, new GsonBuilder().setPrettyPrinting().create().toJson(report), StandardCharsets.UTF_8);
        return new WrittenReports(text, json);
    }
    private List<String> textLines(ModAuditResult audit) {
        List<String> lines = new ArrayList<>(); lines.add("GradleMC Installed Mod Audit"); lines.add("Generated: " + audit.completedAt()); lines.add("Loaded mods: " + audit.snapshot().mods().size()); lines.add("Owning mod files: " + audit.snapshot().owningFileCount()); lines.add("");
        for (ModDescriptor mod : audit.snapshot().mods()) { String jar = GradleMCConfig.MOD_AUDIT_INCLUDE_JAR_FILENAMES.get() ? mod.jarFileName() : ""; lines.add(mod.modId() + " | " + mod.displayName() + " | " + mod.version() + (jar.isBlank() ? "" : " | " + jar)); for (ModDependencyDescriptor dep : mod.dependencies()) lines.add("  dependency: " + dep.modId() + " " + dep.versionRange() + " mandatory=" + dep.mandatory() + " side=" + dep.side()); }
        lines.add(""); lines.add("Findings"); for (ModAuditFinding finding : audit.findings()) { lines.add("[" + finding.severity() + "] " + finding.id() + " (" + finding.confidence() + ", " + finding.basis() + ") - " + finding.title()); lines.add("  " + finding.explanation()); lines.add("  Next: " + finding.recommendedAction()); }
        lines.add(""); lines.add("Limitations: local metadata and observations are evidence, not automatic blame. Use controlled tests to confirm hypotheses."); return lines;
    }
    private List<Map<String, Object>> reportMods(List<ModDescriptor> mods) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ModDescriptor mod : mods) { Map<String, Object> value = new java.util.TreeMap<>(); value.put("modId", mod.modId()); value.put("namespace", mod.namespace()); value.put("displayName", mod.displayName()); value.put("version", mod.version()); value.put("dependencies", mod.dependencies()); value.put("metadataObservations", mod.metadataObservations()); value.put("bundledEntries", mod.entriesInOwningFile()); if (GradleMCConfig.MOD_AUDIT_INCLUDE_JAR_FILENAMES.get()) value.put("jarFileName", mod.jarFileName()); result.add(value); }
        return result;
    }
    private static final class GradleMCVersion { static String version() { return net.minecraftforge.fml.ModList.get().getModContainerById(GradleMC.MOD_ID).map(c -> c.getModInfo().getVersion().toString()).orElse("1.0.3"); } }
}

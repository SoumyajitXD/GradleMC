package com.soumyajit.gradlemc.modaudit;

import com.soumyajit.gradlemc.report.ReportFileNames;
import com.soumyajit.gradlemc.util.AtomicFiles;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.RedactionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Bounded local-only TXT and JSON export of Fabric Loader metadata, never arbitrary JAR contents. */
public final class FabricModAuditReportWriter {
    public List<Path> write(FabricModAuditService.Audit audit, Path directory) throws IOException {
        Files.createDirectories(directory);
        Instant now = Instant.now();
        Path text = ReportFileNames.unique(directory, "gradlemc-mod-audit-", now, ".txt");
        Path json = ReportFileNames.unique(directory, "gradlemc-mod-audit-", now, ".json");
        RedactionService redactor = new RedactionService(GradleMcPaths.gameDirectory(), homeDirectory());
        AtomicFiles.writeUtf8(text, redactor.redact(String.join(System.lineSeparator(), textLines(audit)) + System.lineSeparator()).text());
        AtomicFiles.writeUtf8(json, redactor.redact(jsonFor(audit)).text());
        return List.of(text, json);
    }

    private static List<String> textLines(FabricModAuditService.Audit audit) {
        List<String> lines = new ArrayList<>();
        lines.add("GradleMC Fabric Installed-Mod Audit");
        lines.add("==================================");
        lines.add("Loader: Fabric");
        lines.add("Loaded mods: " + audit.mods().size());
        lines.add("Required dependency declarations: " + audit.requiredDependencyCount());
        lines.add("Findings: " + audit.findings().size());
        lines.add("");
        for (FabricModAuditService.ModDescriptor mod : audit.mods()) {
            lines.add(mod.id() + " | " + unavailable(mod.name()) + " | " + unavailable(mod.version()) + " | " + mod.environment());
            lines.add("  Origin (" + mod.originKind() + "): " + (mod.origins().isEmpty() ? "unavailable" : String.join(", ", mod.origins())));
            if (!mod.nestedIn().isBlank()) lines.add("  Nested in: " + mod.nestedIn());
            if (!mod.containedMods().isEmpty()) lines.add("  Contains: " + String.join(", ", mod.containedMods()));
            if (!mod.authors().isEmpty()) lines.add("  Authors: " + String.join(", ", mod.authors()));
            if (!mod.contacts().isEmpty()) lines.add("  Contacts: " + mod.contacts().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((left, right) -> left + ", " + right).orElse("unavailable"));
            for (FabricModAuditService.Dependency dependency : mod.dependencies()) {
                lines.add("  Dependency: " + dependency.kind() + " " + dependency.id() + " " + dependency.constraints()
                        + " (required=" + dependency.required() + ", loaded=" + dependency.targetLoaded()
                        + ", versionMatch=" + unavailable(dependency.matchesLoadedVersion()) + ")");
            }
        }
        lines.add("");
        lines.add("Findings");
        lines.add("--------");
        if (audit.findings().isEmpty()) lines.add("No missing, incompatible, duplicate, or incomplete loaded metadata was observed.");
        else audit.findings().forEach(finding -> lines.add("[" + finding.kind() + "] " + finding.modId() + ": " + finding.detail()));
        return lines;
    }

    static String jsonFor(FabricModAuditService.Audit audit) {
        StringBuilder out = new StringBuilder("{\n  \"schema\": \"gradlemc.mod_audit.v1\",\n  \"loader\": \"Fabric\",\n  \"mods\": [");
        for (int i = 0; i < audit.mods().size(); i++) {
            FabricModAuditService.ModDescriptor mod = audit.mods().get(i);
            if (i > 0) out.append(',');
            out.append("\n    {\"id\":\"").append(escape(mod.id())).append("\",\"name\":\"").append(escape(mod.name()))
                    .append("\",\"version\":\"").append(escape(mod.version())).append("\",\"description\":\"")
                    .append(escape(mod.description())).append("\",\"environment\":\"").append(escape(mod.environment()))
                    .append("\",\"originKind\":\"").append(escape(mod.originKind())).append("\",\"originFiles\":[");
            for (int j = 0; j < mod.origins().size(); j++) { if (j > 0) out.append(','); out.append('\"').append(escape(mod.origins().get(j))).append('\"'); }
            out.append("],\"nestedIn\":").append(nullableString(mod.nestedIn())).append(",\"containedMods\":");
            appendStrings(out, mod.containedMods());
            out.append(",\"authors\":");
            appendStrings(out, mod.authors());
            out.append(",\"contacts\":");
            appendContacts(out, mod.contacts());
            out.append(",\"dependencies\":[");
            for (int j = 0; j < mod.dependencies().size(); j++) {
                FabricModAuditService.Dependency dependency = mod.dependencies().get(j);
                if (j > 0) out.append(',');
                out.append("{\"id\":\"").append(escape(dependency.id())).append("\",\"kind\":\"")
                        .append(escape(dependency.kind())).append("\",\"positive\":").append(dependency.positive())
                        .append(",\"required\":").append(dependency.required()).append(",\"targetLoaded\":").append(dependency.targetLoaded())
                        .append(",\"constraints\":\"").append(escape(dependency.constraints())).append("\",\"matchesLoadedVersion\":")
                        .append(dependency.matchesLoadedVersion() == null ? "null" : dependency.matchesLoadedVersion()).append('}');
            }
            out.append("]}");
        }
        out.append("\n  ],\n  \"findings\":[");
        for (int i = 0; i < audit.findings().size(); i++) {
            FabricModAuditService.Finding finding = audit.findings().get(i);
            if (i > 0) out.append(',');
            out.append("{\"kind\":\"").append(escape(finding.kind())).append("\",\"modId\":\"")
                    .append(escape(finding.modId())).append("\",\"detail\":\"").append(escape(finding.detail())).append("\"}");
        }
        return out.append("]\n}\n").toString();
    }

    private static void appendStrings(StringBuilder out, List<String> values) {
        out.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) out.append(',');
            out.append('\"').append(escape(values.get(index))).append('\"');
        }
        out.append(']');
    }

    private static void appendContacts(StringBuilder out, Map<String, String> contacts) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : contacts.entrySet()) {
            if (!first) out.append(',');
            out.append('\"').append(escape(entry.getKey())).append("\":\"").append(escape(entry.getValue())).append('\"');
            first = false;
        }
        out.append('}');
    }

    private static String nullableString(String value) { return value == null || value.isBlank() ? "null" : "\"" + escape(value) + "\""; }
    private static String unavailable(Object value) { return value == null || String.valueOf(value).isBlank() ? "unavailable" : String.valueOf(value); }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '\"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) escaped.append(String.format("\\u%04x", (int) character));
                    else escaped.append(character);
                }
            }
        }
        return escaped.toString();
    }
    private static Path homeDirectory() { String home = System.getProperty("user.home"); return home == null || home.isBlank() ? null : Path.of(home); }
}

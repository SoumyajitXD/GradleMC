package com.soumyajit.gradlemc.modaudit;

import com.soumyajit.gradlemc.check.Severity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.soumyajit.gradlemc.rules.RiskRuleLoader;
import com.soumyajit.gradlemc.config.GradleMCConfig;

/** Cheap metadata-only audit. It never opens, hashes, or scans mod jars. */
public final class ModAuditService {
    private static volatile ModAuditResult cached;
    private ModAuditService() { }
    public static ModAuditResult current() { ModAuditResult result = cached; return result == null ? refresh() : new ModAuditResult(result.snapshot(), result.dependencyGraph(), result.findings(), result.completedAt(), true); }
    public static synchronized ModAuditResult refresh() {
        if (!GradleMCConfig.MOD_AUDIT_ENABLED.get()) {
            InstalledModSnapshot unavailable = InstalledModSnapshot.unavailable("Disabled by GradleMC configuration.");
            cached = new ModAuditResult(unavailable, DependencyGraph.of(unavailable), List.of(), Instant.now(), false);
            return cached;
        }
        InstalledModSnapshot snapshot = ForgeInstalledModSnapshotProvider.refresh();
        List<ModAuditFinding> findings = new ArrayList<>();
        if (!snapshot.available()) findings.add(finding(Severity.WARN, "availability", "snapshot-unavailable", "Loaded-mod metadata is unavailable", snapshot.unavailableReason(), List.of(), AuditConfidence.INFORMATIONAL, "Try /gradlemc mods audit refresh after Forge finishes loading.", "observed"));
        for (ModDescriptor mod : snapshot.mods()) {
            for (String observation : mod.metadataObservations()) findings.add(finding(Severity.INFO, "metadata", "metadata-" + mod.modId() + "-" + findings.size(), "Metadata could be more precise", observation, List.of(mod.modId()), AuditConfidence.INFORMATIONAL, "Review this mod's published metadata if you maintain the pack.", "observed"));
            if (!mod.namespace().equals(mod.modId())) findings.add(finding(Severity.INFO, "metadata", "namespace-" + mod.modId(), "Namespace differs from mod ID", "Forge reports namespace '" + mod.namespace() + "' for mod ID '" + mod.modId() + "'.", List.of(mod.modId()), AuditConfidence.CONFIRMED, "Use the recorded namespace for registry attribution; this is not itself a fault.", "exact"));
        }
        Map<String, List<String>> reverse = DependencyGraph.of(snapshot).reverse();
        reverse.forEach((id, dependants) -> { if (dependants.size() >= 5) findings.add(finding(Severity.INFO, "dependency", "structural-" + id, "Structurally important dependency", dependants.size() + " loaded mods declare a direct dependency on this mod.", List.of(id), AuditConfidence.CONFIRMED, "Keep its version and compatibility notes visible when changing the pack; this does not indicate a performance problem.", "exact")); });
        RiskRuleLoader.current().rules().forEach(rule -> {
            if (AuditRuleEvaluator.matches(rule, snapshot)) findings.add(finding(rule.severity(), "local-rule", "rule-" + rule.id(), rule.message(), "A local GradleMC rule matched installed Forge metadata.", rule.modIds().isEmpty() ? List.of(rule.modId()) : rule.modIds(), AuditConfidence.STRONG_EVIDENCE, rule.suggestion(), "rule-based"));
        });
        cached = new ModAuditResult(snapshot, DependencyGraph.of(snapshot), findings, Instant.now(), false);
        return cached;
    }
    private static ModAuditFinding finding(Severity severity, String category, String id, String title, String explanation, List<String> mods, AuditConfidence confidence, String action, String basis) { return new ModAuditFinding(severity, category, id, title, explanation, mods, List.of(explanation), confidence, action, basis); }
}

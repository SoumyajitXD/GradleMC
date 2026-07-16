package com.soumyajit.gradlemc.modaudit;

import com.soumyajit.gradlemc.rules.RiskRule;
import com.soumyajit.gradlemc.rules.RiskRuleSet;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import java.util.List;

/** Evaluates local declarative rules against the same immutable snapshot used by all audit consumers. */
final class AuditRuleEvaluator {
    private AuditRuleEvaluator() { }
    static boolean matches(RiskRule rule, InstalledModSnapshot snapshot) {
        boolean present = snapshot.find(rule.modId()).isPresent();
        return switch (rule.type()) {
            case MOD_PRESENT -> present;
            case MOD_MISSING -> !present;
            case ALL_MODS_PRESENT -> rule.modIds().stream().allMatch(id -> snapshot.find(id).isPresent());
            case ANY_MOD_PRESENT -> rule.modIds().stream().anyMatch(id -> snapshot.find(id).isPresent());
            case MOD_GROUP_COUNT -> rule.modIds().stream().filter(id -> snapshot.find(id).isPresent()).count() > rule.maxPresent();
            case DEPENDENCY_PRESENT -> snapshot.find(rule.modId()).map(mod -> mod.dependencies().stream().anyMatch(dep -> dep.modId().equals(rule.dependencyModId()))).orElse(false);
            case DEPENDENCY_ABSENT -> snapshot.find(rule.modId()).map(mod -> mod.dependencies().stream().noneMatch(dep -> dep.modId().equals(rule.dependencyModId()))).orElse(false);
            case MOD_VERSION_IN_RANGE -> inRange(snapshot.find(rule.modId()).map(ModDescriptor::version).orElse(""), rule.versionRange());
            case MOD_VERSION_OUTSIDE_RANGE -> present && !inRange(snapshot.find(rule.modId()).map(ModDescriptor::version).orElse(""), rule.versionRange());
            case CONFIG_FILE_EXISTS, CLIENT_ONLY_ON_SERVER, SERVER_ONLY_ON_CLIENT -> false;
        };
    }
    private static boolean inRange(String version, String range) {
        try { return VersionRange.createFromVersionSpec(range).containsVersion(new DefaultArtifactVersion(version)); }
        catch (Exception ignored) { return false; }
    }
}

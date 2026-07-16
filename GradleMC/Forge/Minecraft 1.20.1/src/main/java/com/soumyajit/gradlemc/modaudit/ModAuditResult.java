package com.soumyajit.gradlemc.modaudit;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ModAuditResult(InstalledModSnapshot snapshot, DependencyGraph dependencyGraph, List<ModAuditFinding> findings,
                             Instant completedAt, boolean fromCache) {
    public ModAuditResult { findings = findings == null ? List.of() : findings.stream().sorted(Comparator.comparing(ModAuditFinding::id)).toList(); completedAt = completedAt == null ? Instant.now() : completedAt; }
}

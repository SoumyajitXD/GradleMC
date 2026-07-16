package com.soumyajit.gradlemc.modaudit;

import com.soumyajit.gradlemc.check.Severity;
import java.util.List;

public record ModAuditFinding(Severity severity, String category, String id, String title, String explanation,
                              List<String> affectedModIds, List<String> evidence, AuditConfidence confidence,
                              String recommendedAction, String basis) {
    public ModAuditFinding { affectedModIds = affectedModIds == null ? List.of() : List.copyOf(affectedModIds); evidence = evidence == null ? List.of() : List.copyOf(evidence); }
}

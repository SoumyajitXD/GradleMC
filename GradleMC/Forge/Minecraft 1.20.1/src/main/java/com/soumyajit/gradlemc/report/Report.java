package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;

import java.time.Instant;
import java.util.List;

public record Report(String title, Instant createdAt, List<CheckResult> results) {
    public long count(Severity severity) {
        return results.stream()
                .filter(result -> result.severity() == severity)
                .count();
    }

    public long warningCount() {
        return count(Severity.WARN);
    }

    public long failureCount() {
        return count(Severity.FAIL) + count(Severity.CRITICAL);
    }

    public String summaryLine() {
        return count(Severity.PASS) + " pass, "
                + count(Severity.INFO) + " info, "
                + warningCount() + " warn, "
                + failureCount() + " fail";
    }
}

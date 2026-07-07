package com.soumyajit.gradlemc.check;

public record CheckResult(
        Severity severity,
        CheckCategory category,
        String title,
        String detail,
        String suggestion
) {
    public static CheckResult of(Severity severity, CheckCategory category, String title, String detail, String suggestion) {
        return new CheckResult(severity, category, title, detail, suggestion);
    }
}

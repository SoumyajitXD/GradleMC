package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.util.GradleMcPaths;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Central privacy redaction for standalone human-readable reports. */
public final class ReportRedactor {
    private ReportRedactor() { }
    public static String redact(String value) {
        if (value == null) return "";
        String safe = value;
        try {
            safe = replace(safe, GradleMcPaths.gameDirectory().toAbsolutePath().normalize().toString(), "[game-dir]");
        } catch (RuntimeException unavailable) {
            // Dependency-free self-tests and early bootstrap do not yet have an FML game path.
        }
        String home = System.getProperty("user.home", "");
        if (!home.isBlank()) safe = replace(safe, Path.of(home).toAbsolutePath().normalize().toString(), "[user-home]");
        return DiagnosticRedactor.redact(safe);
    }
    private static String replace(String value, String literal, String replacement) {
        if (literal.isBlank()) return value;
        return Pattern.compile(Pattern.quote(literal), Pattern.CASE_INSENSITIVE).matcher(value)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }
}

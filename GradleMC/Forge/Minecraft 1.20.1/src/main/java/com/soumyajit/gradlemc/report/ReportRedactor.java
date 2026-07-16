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
        String safe = replace(value, GradleMcPaths.gameDirectory().toAbsolutePath().normalize().toString(), "[game-dir]");
        String home = System.getProperty("user.home", "");
        if (!home.isBlank()) safe = replace(safe, Path.of(home).toAbsolutePath().normalize().toString(), "[user-home]");
        safe = safe.replaceAll("(?i)(token|password|api[_-]?key|secret)\\s*[=:]\\s*[^\\s,;]+", "$1=[redacted]");
        return safe.replaceAll("(?i)\\b[A-Z]:[\\\\/][^\\s\\\"']+", "[absolute-path]");
    }
    private static String replace(String value, String literal, String replacement) {
        if (literal.isBlank()) return value;
        return Pattern.compile(Pattern.quote(literal), Pattern.CASE_INSENSITIVE).matcher(value)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }
}

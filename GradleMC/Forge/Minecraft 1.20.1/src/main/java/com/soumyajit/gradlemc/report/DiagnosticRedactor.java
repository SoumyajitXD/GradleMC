package com.soumyajit.gradlemc.report;

import java.util.regex.Pattern;

/** Small deterministic policy shared by every persistent diagnostic text sink. */
public final class DiagnosticRedactor {
    private static final Pattern SECRET = Pattern.compile("(?i)\\b(token|password|passphrase|api[_-]?key|secret|authorization|cookie|access[_-]?key)\\s*[:=]\\s*(?:bearer\\s+)?[^\\s,;]+" );
    private static final Pattern BEARER = Pattern.compile("(?i)\\bbearer\\s+[a-z0-9._~+/-]+=*");
    private static final Pattern USERINFO = Pattern.compile("(?i)([a-z][a-z0-9+.-]*://)[^\\s/@:]+(?::[^\\s/@]*)?@");
    private static final Pattern WINDOWS_PATH = Pattern.compile("(?i)(?:[a-z]:[\\\\/]|\\\\\\\\(?:\\?\\\\)?(?:unc\\\\)?[^\\\\/]+[\\\\/][^\\\\/]+)[^\\s\\\"']*");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private DiagnosticRedactor() { }

    public static String redact(String value) {
        if (value == null) return "";
        String safe = CONTROL.matcher(value).replaceAll("?");
        safe = USERINFO.matcher(safe).replaceAll("$1[redacted]@");
        safe = SECRET.matcher(safe).replaceAll("$1=[redacted]");
        safe = BEARER.matcher(safe).replaceAll("Bearer [redacted]");
        return WINDOWS_PATH.matcher(safe).replaceAll("[absolute-path]");
    }
}

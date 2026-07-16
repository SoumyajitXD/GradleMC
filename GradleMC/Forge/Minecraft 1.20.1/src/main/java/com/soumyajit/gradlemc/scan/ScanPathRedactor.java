package com.soumyajit.gradlemc.scan;

import java.nio.file.Path;
import java.util.Locale;

/** Central display-only path policy for Scan v1.  It never returns an absolute private path. */
public final class ScanPathRedactor {
    private ScanPathRedactor() { }

    public static String redact(Path value, Path gameDirectory) {
        if (value == null) return "";
        Path normalized = value.toAbsolutePath().normalize();
        if (gameDirectory != null) {
            Path game = gameDirectory.toAbsolutePath().normalize();
            if (normalized.startsWith(game)) return "<gameDir>\\" + game.relativize(normalized).toString().replace('/', '\\');
        }
        Path file = normalized.getFileName();
        return "<redacted>\\" + (file == null ? "external-file" : safeName(file.toString()));
    }

    public static String redactText(String value, Path gameDirectory) {
        if (value == null || value.isBlank()) return "";
        String safe = value.replace('/', '\\');
        if (gameDirectory != null) {
            String game = gameDirectory.toAbsolutePath().normalize().toString().replace('/', '\\');
            if (!game.isBlank()) safe = safe.replaceAll("(?i)" + java.util.regex.Pattern.quote(game), "<gameDir>");
        }
        String home = System.getProperty("user.home", "").replace('/', '\\');
        if (!home.isBlank()) safe = safe.replaceAll("(?i)" + java.util.regex.Pattern.quote(home), "<redacted>");
        return safe.replaceAll("(?i)\\b[a-z]:\\\\(?:[^\\r\\n\\\"']*)", "<redacted>");
    }

    private static String safeName(String value) {
        String text = value.replaceAll("[\\\\/:*?\"<>|]", "_");
        return text.length() <= 160 ? text : text.substring(0, 160) + "…";
    }
}

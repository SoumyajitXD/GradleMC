package com.soumyajit.gradlemc.util;

import java.nio.file.Path;
import java.util.regex.Pattern;

/** Best-effort bounded redaction.  Collection is minimized before this is used. */
public final class RedactionService {
    private static final int MAX_SCAN_CHARS = 64 * 1024;
    private static final Pattern AUTHORIZATION = Pattern.compile("(?i)(authorization\\s*[:=]\\s*(?:bearer|basic)\\s+)[^\\s,;]+" );
    private static final Pattern SECRET_VALUE = Pattern.compile("(?i)\\b(token|password|api[_-]?key|secret|session(?:id)?|cookie)\\s*[:=]\\s*([^\\s,;]+)");
    private static final Pattern WEBHOOK = Pattern.compile("(?i)(https?://[^\\s/]+/api/webhooks/)[^\\s]+" );
    private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN (?:[A-Z ]+ )?PRIVATE KEY-----");
    private final Path gameDirectory;
    private final Path homeDirectory;

    public RedactionService(Path gameDirectory, Path homeDirectory) {
        this.gameDirectory = gameDirectory == null ? null : gameDirectory.toAbsolutePath().normalize();
        this.homeDirectory = homeDirectory == null ? null : homeDirectory.toAbsolutePath().normalize();
    }

    public Result redact(String input) {
        if (input == null || input.isEmpty()) return new Result("", 0, false);
        boolean truncated = input.length() > MAX_SCAN_CHARS;
        String value = truncated ? input.substring(0, MAX_SCAN_CHARS) + " [truncated before redaction]" : input;
        int count = 0;
        String before = value;
        value = AUTHORIZATION.matcher(value).replaceAll("$1<redacted>"); if (!before.equals(value)) count++;
        before = value; value = SECRET_VALUE.matcher(value).replaceAll("$1=<redacted>"); if (!before.equals(value)) count++;
        before = value; value = WEBHOOK.matcher(value).replaceAll("$1<redacted>"); if (!before.equals(value)) count++;
        before = value; value = PRIVATE_KEY.matcher(value).replaceAll("<redacted-private-key>"); if (!before.equals(value)) count++;
        if (gameDirectory != null) { before = value; value = value.replace(gameDirectory.toString(), "<gameDir>"); if (!before.equals(value)) count++; }
        if (homeDirectory != null) { before = value; value = value.replace(homeDirectory.toString(), "<home>"); if (!before.equals(value)) count++; }
        return new Result(value, count, truncated);
    }

    public record Result(String text, int redactionCount, boolean truncated) { }
}

package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.util.GradleMcLimits;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ReportFileNames {
    public static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault());
    public static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());

    private ReportFileNames() {
    }

    public static Path unique(Path directory, String prefix, Instant instant, String extension) throws IOException {
        Path path = directory.resolve(prefix + FILE_TIMESTAMP.format(instant) + extension);
        int duplicate = 1;
        while (Files.exists(path) && duplicate <= GradleMcLimits.MAX_REPORT_NAME_COLLISION_ATTEMPTS) {
            path = directory.resolve(prefix + FILE_TIMESTAMP.format(instant) + "-" + duplicate + extension);
            duplicate++;
        }
        if (Files.exists(path)) throw new IOException("Too many existing GradleMC reports share this timestamp");
        return path;
    }
}

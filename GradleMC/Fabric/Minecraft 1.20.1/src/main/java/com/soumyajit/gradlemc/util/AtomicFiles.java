package com.soumyajit.gradlemc.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Small, injectable-free atomic-replacement helper for GradleMC-owned files. */
public final class AtomicFiles {
    private AtomicFiles() { }

    public static void writeUtf8(Path target, String content) throws IOException {
        if (target == null || content == null) throw new IllegalArgumentException("target and content are required");
        Path parent = target.getParent();
        if (parent == null) throw new IOException("GradleMC destination has no parent");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".gradlemc-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            force(temporary);
            replace(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void replace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (AtomicMoveNotSupportedException ignored) {
            // The fallback is explicitly non-atomic.  The complete temporary file exists before replacement.
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void force(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }
}

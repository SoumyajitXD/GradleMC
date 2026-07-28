package com.soumyajit.gradlemc.investigation.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Atomic replacement for an already-validated, owned regular target. */
final class AtomicUtf8File {
    interface Operations {
        Path createTempFile(Path parent) throws IOException;

        void writeAndForce(Path temp, byte[] bytes) throws IOException;

        void move(Path source, Path target, boolean atomic) throws IOException;

        void deleteIfExists(Path path) throws IOException;
    }

    private static final Operations FILES = new Operations() {
        @Override
        public Path createTempFile(Path parent) throws IOException {
            return Files.createTempFile(parent, ".investigation-", ".tmp");
        }

        @Override
        public void writeAndForce(Path temp, byte[] bytes) throws IOException {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true);
            }
        }

        @Override
        public void move(Path source, Path target, boolean atomic) throws IOException {
            if (atomic) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        @Override
        public void deleteIfExists(Path path) throws IOException {
            Files.deleteIfExists(path);
        }
    };

    private static volatile Operations operations = FILES;

    private AtomicUtf8File() {
    }

    static boolean write(Path target, byte[] bytes) throws IOException {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes");
        }
        Path parent = target.getParent();
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(parent)) {
            throw new IOException("Unsafe target directory");
        }
        Path realParent = parent.toRealPath();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                && (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("Unsafe target file");
        }
        Path temp = operations.createTempFile(parent);
        IOException primaryFailure = null;
        try {
            operations.writeAndForce(temp, bytes);
            try {
                operations.move(temp, target, true);
                verifyWrittenTarget(target, realParent);
                return false;
            } catch (AtomicMoveNotSupportedException unsupported) {
                operations.move(temp, target, false);
                verifyWrittenTarget(target, realParent);
                return true;
            }
        } catch (IOException exception) {
            primaryFailure = exception;
            throw exception;
        } finally {
            try {
                operations.deleteIfExists(temp);
            } catch (IOException exception) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(exception);
                } else if (Files.exists(temp, LinkOption.NOFOLLOW_LINKS)) {
                    throw exception;
                }
            }
        }
    }

    static void setOperationsForTests(Operations value) {
        operations = value == null ? FILES : value;
    }

    private static void verifyWrittenTarget(Path target, Path realParent) throws IOException {
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(target)
                || !target.getParent().toRealPath().equals(realParent)) {
            throw new IOException("Managed target changed while being written");
        }
    }
}

package com.soumyajit.gradlemc.investigation.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic failure-injection coverage for the owned Investigation writer. */
public final class AtomicUtf8FileSelfTest {
    private AtomicUtf8FileSelfTest() {
    }

    public static void run() throws Exception {
        Path root = Files.createTempDirectory("gradlemc-atomic-test-");
        try {
            Path target = root.resolve("manifest.json");
            Files.writeString(target, "original", StandardCharsets.UTF_8);
            writesAndFallsBack(target);
            preservesOriginalOnTempCreationFailure(target);
            preservesOriginalOnWriteFailure(target);
            preservesOriginalOnMoveFailure(target);
            preservesOriginalOnFallbackFailure(target);
            preservesPrimaryFailureWhenCleanupAlsoFails(target);
        } finally {
            AtomicUtf8File.setOperationsForTests(null);
            deleteTree(root);
        }
    }

    private static void preservesOriginalOnTempCreationFailure(Path target) throws Exception {
        Files.writeString(target, "original", StandardCharsets.UTF_8);
        AtomicUtf8File.setOperationsForTests(new DelegatingOps() {
            @Override public Path createTempFile(Path parent) throws IOException { throw new IOException("injected temp failure"); }
        });
        fails(() -> AtomicUtf8File.write(target, "replacement".getBytes(StandardCharsets.UTF_8)));
        check("original".equals(Files.readString(target, StandardCharsets.UTF_8)));
    }

    private static void writesAndFallsBack(Path target) throws Exception {
        AtomicInteger moves = new AtomicInteger();
        AtomicUtf8File.setOperationsForTests(new DelegatingOps() {
            @Override public void move(Path source, Path destination, boolean atomic) throws IOException {
                if (atomic && moves.getAndIncrement() == 0) throw new AtomicMoveNotSupportedException(source.toString(), destination.toString(), "test");
                super.move(source, destination, atomic);
            }
        });
        check(AtomicUtf8File.write(target, "fallback".getBytes(StandardCharsets.UTF_8)));
        check("fallback".equals(Files.readString(target, StandardCharsets.UTF_8)));
    }

    private static void preservesOriginalOnWriteFailure(Path target) throws Exception {
        Files.writeString(target, "original", StandardCharsets.UTF_8);
        AtomicUtf8File.setOperationsForTests(new DelegatingOps() {
            @Override public void writeAndForce(Path temp, byte[] bytes) throws IOException { throw new IOException("injected write failure"); }
        });
        fails(() -> AtomicUtf8File.write(target, "replacement".getBytes(StandardCharsets.UTF_8)));
        check("original".equals(Files.readString(target, StandardCharsets.UTF_8)));
    }

    private static void preservesOriginalOnMoveFailure(Path target) throws Exception {
        Files.writeString(target, "original", StandardCharsets.UTF_8);
        AtomicUtf8File.setOperationsForTests(new DelegatingOps() {
            @Override public void move(Path source, Path destination, boolean atomic) throws IOException { throw new IOException("injected move failure"); }
        });
        fails(() -> AtomicUtf8File.write(target, "replacement".getBytes(StandardCharsets.UTF_8)));
        check("original".equals(Files.readString(target, StandardCharsets.UTF_8)));
    }

    private static void preservesOriginalOnFallbackFailure(Path target) throws Exception {
        Files.writeString(target, "original", StandardCharsets.UTF_8);
        AtomicUtf8File.setOperationsForTests(new DelegatingOps() {
            @Override public void move(Path source, Path destination, boolean atomic) throws IOException {
                if (atomic) throw new AtomicMoveNotSupportedException(source.toString(), destination.toString(), "test");
                throw new IOException("injected fallback failure");
            }
        });
        fails(() -> AtomicUtf8File.write(target, "replacement".getBytes(StandardCharsets.UTF_8)));
        check("original".equals(Files.readString(target, StandardCharsets.UTF_8)));
    }

    private static void preservesPrimaryFailureWhenCleanupAlsoFails(Path target) throws Exception {
        Files.writeString(target, "original", StandardCharsets.UTF_8);
        AtomicUtf8File.setOperationsForTests(new DelegatingOps() {
            @Override public void writeAndForce(Path temp, byte[] bytes) throws IOException { throw new IOException("primary write failure"); }
            @Override public void deleteIfExists(Path path) throws IOException { throw new IOException("cleanup failure"); }
        });
        try {
            AtomicUtf8File.write(target, "replacement".getBytes(StandardCharsets.UTF_8));
            throw new AssertionError("expected failure");
        } catch (IOException expected) {
            check("primary write failure".equals(expected.getMessage()));
            check(expected.getSuppressed().length == 1);
        }
        check("original".equals(Files.readString(target, StandardCharsets.UTF_8)));
    }

    private static class DelegatingOps implements AtomicUtf8File.Operations {
        @Override public Path createTempFile(Path parent) throws IOException { return Files.createTempFile(parent, ".investigation-", ".tmp"); }
        @Override public void writeAndForce(Path temp, byte[] bytes) throws IOException { Files.write(temp, bytes); }
        @Override public void move(Path source, Path target, boolean atomic) throws IOException {
            if (atomic) Files.move(source, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            else Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        @Override public void deleteIfExists(Path path) throws IOException { Files.deleteIfExists(path); }
    }

    private static void fails(ThrowingRunnable runnable) throws Exception {
        try { runnable.run(); throw new AssertionError("expected failure"); } catch (IOException expected) { }
    }
    private static void check(boolean value) { if (!value) throw new AssertionError("AtomicUtf8File self-test failed"); }
    private static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException exception) { throw new RuntimeException(exception); } }); }
    }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}

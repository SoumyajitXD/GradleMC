package com.soumyajit.gradlemc.task;

import java.io.IOException;
import java.nio.file.*;

/** Small shared filesystem boundary for history/cache. All inputs are local but treated as untrusted. */
final class ManagedJsonFiles {
    private ManagedJsonFiles() { }
    static Path root(Path root) throws IOException {
        Path value = root.toAbsolutePath().normalize();
        Files.createDirectories(value);
        if (Files.isSymbolicLink(value)) throw new IOException("Managed directory cannot be a symbolic link");
        return value;
    }
    static Path child(Path root, String name) throws IOException {
        if (name == null || !name.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}")) throw new IOException("Unsafe managed filename");
        Path value = root(root).resolve(name).normalize();
        if (!value.startsWith(root(root))) throw new IOException("Managed path escape rejected");
        return value;
    }
    static void writeAtomically(Path root, String name, byte[] bytes, long maxBytes) throws IOException {
        if (bytes.length > maxBytes) throw new IOException("Managed JSON exceeds size limit");
        Path managedRoot = root(root); Path target = child(managedRoot, name);
        Path temporary = Files.createTempFile(managedRoot, "." + name + ".", ".tmp");
        try {
            Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}

package com.soumyajit.gradlemc.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Creates a managed directory without accepting symlinked path components below its trusted base. */
public final class ManagedPathSafety {
    private ManagedPathSafety() { }

    public static Path ensureDirectory(Path trustedBase, Path requestedDirectory) throws IOException {
        Path base = trustedBase.toAbsolutePath().normalize();
        Path directory = requestedDirectory.toAbsolutePath().normalize();
        if (!directory.startsWith(base)) throw new IOException("Managed directory escaped its trusted base");
        if (!Files.isDirectory(base)) throw new IOException("Trusted base directory is unavailable");
        Path current = base;
        for (Path component : base.relativize(directory)) {
            current = current.resolve(component);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Managed directory contains a symbolic link or non-directory component");
                }
            } else {
                Files.createDirectory(current);
            }
        }
        Path realBase = base.toRealPath();
        Path realDirectory = directory.toRealPath();
        if (!realDirectory.startsWith(realBase)) throw new IOException("Managed directory resolved outside its trusted base");
        return directory;
    }
}

package com.soumyajit.gradlemc.investigation.storage;

import com.soumyajit.gradlemc.investigation.InvestigationId;
import com.soumyajit.gradlemc.util.ManagedPathSafety;
import java.io.IOException;
import java.nio.file.*;

public final class InvestigationPaths {
    private InvestigationPaths() { }
    public static Path root(Path game) throws IOException {
        Path base = game.toAbsolutePath().normalize();
        return ManagedPathSafety.ensureDirectory(base, base.resolve("gradlemc").resolve("investigations"));
    }
    public static Path session(Path game, InvestigationId id) throws IOException {
        Path root = root(game);
        Path session = root.resolve(id.value()).normalize();
        if (!session.startsWith(root)) throw new IOException("Investigation path escaped root");
        return session;
    }
    public static Path manifest(Path game, InvestigationId id) throws IOException { return session(game, id).resolve("manifest.json"); }
    public static Path index(Path game) throws IOException { return root(game).resolve("index.json"); }
    public static void ensureSession(Path game, InvestigationId id) throws IOException {
        Path root = root(game), session = session(game, id);
        if (Files.exists(session, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException("Investigation already exists");
        ManagedPathSafety.ensureDirectory(root, session);
    }
    public static Path requireManifest(Path game, InvestigationId id) throws IOException { return ManagedPathSafety.requireRegularFile(root(game), manifest(game, id)); }
    public static Path requireIndex(Path game) throws IOException { return ManagedPathSafety.requireRegularFile(root(game), index(game)); }
}

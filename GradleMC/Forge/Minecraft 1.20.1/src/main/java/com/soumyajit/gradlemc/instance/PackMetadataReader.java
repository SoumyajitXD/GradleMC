package com.soumyajit.gradlemc.instance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Bounded central-directory inspection. It never extracts an archive or follows an archive entry path. */
public final class PackMetadataReader {
    public static final long MAX_ARCHIVE_BYTES = 128L * 1024L * 1024L;
    public static final int MAX_ENTRIES = 10_000;
    public static final int MAX_METADATA_BYTES = 64 * 1024;
    private PackMetadataReader() { }

    public static PackDescriptor read(Path root, Path file, PackDescriptor.PackKind kind) {
        String name = file.getFileName().toString();
        try {
            if (!file.normalize().startsWith(root.normalize()) || Files.isSymbolicLink(file)) return descriptor(name, kind, false, 0, 0, -1, "rejected", List.of(), List.of("Path rejected outside managed directory or through symlink."));
            long size = Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS) ? boundedDirectorySize(file) : Files.size(file);
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && size > MAX_ARCHIVE_BYTES) return descriptor(name, kind, true, size, modified, -1, "limited", List.of(), List.of("Archive exceeds safe inspection limit."));
            Metadata metadata = Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS) ? directoryMetadata(file) : zipMetadata(file);
            return descriptor(name, kind, !Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS), size, modified, metadata.format, metadata.status, List.copyOf(metadata.namespaces), metadata.warnings);
        } catch (IOException | RuntimeException e) { return descriptor(name, kind, !Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS), 0, 0, -1, "unreadable", List.of(), List.of(e.getClass().getSimpleName())); }
    }
    private static PackDescriptor descriptor(String name, PackDescriptor.PackKind kind, boolean archive, long size, long modified, int format, String status, List<String> namespaces, List<String> warnings) { return new PackDescriptor(name.toLowerCase(Locale.ROOT), name, kind, archive, size, modified, format, status, namespaces, warnings); }
    private static Metadata directoryMetadata(Path dir) throws IOException {
        Path meta = dir.resolve("pack.mcmeta");
        boolean hasMetadata = Files.isRegularFile(meta, LinkOption.NOFOLLOW_LINKS);
        String text = hasMetadata ? readBounded(Files.newInputStream(meta, LinkOption.NOFOLLOW_LINKS)) : "";
        Set<String> namespaces = new TreeSet<>(); Path assets = dir.resolve("assets"); if (Files.isDirectory(assets, LinkOption.NOFOLLOW_LINKS)) try (var entries = Files.list(assets)) { entries.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).limit(256).forEach(p -> namespaces.add(p.getFileName().toString())); }
        return parse(text, namespaces, hasMetadata);
    }
    private static Metadata zipMetadata(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            if (zip.size() > MAX_ENTRIES) return new Metadata(-1, "limited", Set.of(), List.of("Archive entry count exceeds safe inspection limit."));
            ZipEntry meta = zip.getEntry("pack.mcmeta"); String text = meta == null ? "" : readBounded(zip.getInputStream(meta)); Set<String> namespaces = new TreeSet<>();
            Enumeration<? extends ZipEntry> entries = zip.entries(); while(entries.hasMoreElements()) { String name = entries.nextElement().getName(); if (name.startsWith("/") || name.contains("..")) return new Metadata(-1, "rejected", Set.of(), List.of("Unsafe archive entry name.")); if (name.startsWith("assets/")) { String[] parts = name.split("/"); if(parts.length > 2) namespaces.add(parts[1]); } }
            return parse(text, namespaces, meta != null);
        }
    }
    private static Metadata parse(String text, Set<String> namespaces, boolean present) {
        if (!present) return new Metadata(-1, "missing", namespaces, List.of("pack.mcmeta is missing."));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\"pack_format\\\"\\s*:\\s*(\\d+)").matcher(text);
        if (!m.find()) return new Metadata(-1, "malformed", namespaces, List.of("pack.mcmeta does not contain a numeric pack_format."));
        return new Metadata(Integer.parseInt(m.group(1)), "parsed", namespaces, List.of());
    }
    private static String readBounded(InputStream input) throws IOException { try (input) { byte[] bytes = input.readNBytes(MAX_METADATA_BYTES + 1); if (bytes.length > MAX_METADATA_BYTES) throw new IOException("metadata-limit"); return new String(bytes, StandardCharsets.UTF_8); } }
    private static long boundedDirectorySize(Path root) throws IOException { final long[] total = {0}; try (var paths = Files.walk(root, 3)) { for (Path p : paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).limit(MAX_ENTRIES).toList()) { total[0] = Math.min(MAX_ARCHIVE_BYTES + 1, total[0] + Files.size(p)); } } return total[0]; }
    private record Metadata(int format, String status, Set<String> namespaces, List<String> warnings) { }
}

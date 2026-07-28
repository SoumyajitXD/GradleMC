package com.soumyajit.gradlemc.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Bounded local-only catalog. It accepts trusted directories, never remote strings, and performs
 * no work when callers merely read a returned {@link Catalog}.
 */
public final class LocalReportIndex {
    public static final int MAX_DEPTH = 2;
    public static final int MAX_INSPECTED_ENTRIES = 256;
    public static final int MAX_RETAINED_ENTRIES = 96;
    public static final int MAX_FILE_NAME_LENGTH = 128;
    public static final long MAX_SUMMARY_FILE_BYTES = 256L * 1024L;
    public static final int MAX_SUMMARY_LINES = 24;
    public static final int MAX_SUMMARY_BYTES = 12 * 1024;
    private static final Set<String> TEXT_EXTENSIONS = Set.of(".txt", ".json");
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(".zip");

    public enum Kind { FPS, PERFORMANCE, WORLDGEN, PROFILER, EXPORT, ISSUE_BUNDLE, GENERAL }
    public enum State {
        NOT_GENERATED, AVAILABLE, INDEXED_FILE_MISSING, INVALID_PATH, UNSUPPORTED_FORMAT,
        REMOTE_ONLY, OUTPUT_ROOT_UNAVAILABLE, INDEX_STALE, INDEX_LOAD_ERROR
    }
    public enum Action { VIEW_SUMMARY, COPY_PATH, OPEN_FOLDER }

    public record Root(Kind kind, Path directory, Set<String> prefixes, Set<String> extensions) {
        public Root {
            kind = kind == null ? Kind.GENERAL : kind;
            directory = directory == null ? Path.of("") : directory.toAbsolutePath().normalize();
            prefixes = prefixes == null ? Set.of() : Set.copyOf(prefixes);
            extensions = extensions == null ? Set.of() : Set.copyOf(extensions);
        }
    }

    public record Entry(Kind kind, Path path, String displayName, Instant modifiedAt, long size, boolean summarySupported) {
        public Entry {
            kind = kind == null ? Kind.GENERAL : kind;
            path = path == null ? Path.of("") : path.toAbsolutePath().normalize();
            displayName = displayName == null ? "" : displayName;
            modifiedAt = modifiedAt == null ? Instant.EPOCH : modifiedAt;
            size = Math.max(0L, size);
        }
    }

    public record Catalog(Instant refreshedAt, Map<Kind, List<Entry>> entries, Map<Kind, State> states,
                          boolean stale, String error) {
        public Catalog {
            refreshedAt = refreshedAt == null ? Instant.EPOCH : refreshedAt;
            EnumMap<Kind, List<Entry>> safeEntries = new EnumMap<>(Kind.class);
            if (entries != null) for (Map.Entry<Kind, List<Entry>> item : entries.entrySet())
                safeEntries.put(item.getKey(), List.copyOf(item.getValue()));
            entries = Map.copyOf(safeEntries);
            EnumMap<Kind, State> safeStates = new EnumMap<>(Kind.class);
            if (states != null) safeStates.putAll(states);
            for (Kind kind : Kind.values()) safeStates.putIfAbsent(kind, safeEntries.containsKey(kind) && !safeEntries.get(kind).isEmpty()
                    ? State.AVAILABLE : State.NOT_GENERATED);
            states = Map.copyOf(safeStates);
            error = error == null ? "" : error;
        }
        public static Catalog empty() { return new Catalog(Instant.EPOCH, Map.of(), Map.of(), false, ""); }
        public Optional<Entry> latest(Kind kind) { return entries.getOrDefault(kind, List.of()).stream().max(ENTRY_ORDER); }
        public Optional<Entry> latestOverall() { return entries.values().stream().flatMap(List::stream).max(ENTRY_ORDER); }
        public State state(Kind kind) { return stale ? State.INDEX_STALE : states.getOrDefault(kind, State.NOT_GENERATED); }
    }

    private static final Comparator<Entry> ENTRY_ORDER = Comparator.comparing(Entry::modifiedAt)
            .thenComparing(Entry::displayName, String.CASE_INSENSITIVE_ORDER).thenComparing(entry -> entry.path().toString());

    private LocalReportIndex() { }

    public static Catalog scan(List<Root> roots) {
        EnumMap<Kind, List<Entry>> entries = new EnumMap<>(Kind.class);
        EnumMap<Kind, State> states = new EnumMap<>(Kind.class);
        int[] inspected = {0};
        if (roots == null || roots.isEmpty()) return Catalog.empty();
        try {
            for (Root root : roots) scanRoot(root, entries, states, inspected);
            for (List<Entry> value : entries.values()) value.sort(ENTRY_ORDER.reversed());
            return new Catalog(Instant.now(), entries, states, false, "");
        } catch (RuntimeException exception) {
            return staleOrError(entries, states, exception.getClass().getSimpleName());
        }
    }

    private static void scanRoot(Root root, EnumMap<Kind, List<Entry>> entries, EnumMap<Kind, State> states, int[] inspected) {
        if (root.directory().toString().isBlank() || !Files.isDirectory(root.directory(), LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root.directory())) {
            states.putIfAbsent(root.kind(), State.OUTPUT_ROOT_UNAVAILABLE);
            return;
        }
        final Path realRoot;
        try { realRoot = root.directory().toRealPath(LinkOption.NOFOLLOW_LINKS); }
        catch (IOException exception) { states.put(root.kind(), State.OUTPUT_ROOT_UNAVAILABLE); return; }
        try (Stream<Path> paths = Files.walk(root.directory(), MAX_DEPTH)) {
            paths.filter(path -> !path.equals(root.directory())).forEach(path -> {
                if (inspected[0]++ >= MAX_INSPECTED_ENTRIES) return;
                Optional<Entry> entry = inspect(root, realRoot, path);
                if (entry.isPresent()) {
                    List<Entry> values = entries.computeIfAbsent(root.kind(), unused -> new ArrayList<>());
                    if (values.size() < MAX_RETAINED_ENTRIES) values.add(entry.get());
                    states.put(root.kind(), State.AVAILABLE);
                }
            });
        } catch (IOException | SecurityException exception) {
            states.put(root.kind(), State.INDEX_LOAD_ERROR);
        }
    }

    private static Optional<Entry> inspect(Root root, Path realRoot, Path candidate) {
        try {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (!normalized.startsWith(root.directory()) || Files.isSymbolicLink(normalized)
                    || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
            Path relative = root.directory().relativize(normalized);
            if (relative.isAbsolute() || hasTraversal(relative) || relative.getFileName() == null) return Optional.empty();
            String name = relative.getFileName().toString();
            if (name.length() > MAX_FILE_NAME_LENGTH || !matches(root, name)) return Optional.empty();
            Path real = normalized.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!real.startsWith(realRoot) || Files.isDirectory(real, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(real)) return Optional.empty();
            BasicFileAttributes attrs = Files.readAttributes(real, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return Optional.of(new Entry(root.kind(), normalized, name, attrs.lastModifiedTime().toInstant(), attrs.size(),
                    textExtension(name) && attrs.size() <= MAX_SUMMARY_FILE_BYTES));
        } catch (IOException | SecurityException exception) {
            return Optional.empty();
        }
    }

    public static State validateFor(Entry entry, List<Root> roots, Action action) {
        if (entry == null || roots == null) return State.INVALID_PATH;
        Root root = roots.stream().filter(value -> value.kind() == entry.kind()).findFirst().orElse(null);
        if (root == null || !inspect(root, realRoot(root), entry.path()).isPresent()) return State.INDEXED_FILE_MISSING;
        if (action == Action.VIEW_SUMMARY && (!entry.summarySupported() || sizeOverSummaryLimit(entry.path()))) return State.UNSUPPORTED_FORMAT;
        return State.AVAILABLE;
    }

    private static Path realRoot(Root root) {
        try { return root.directory().toRealPath(LinkOption.NOFOLLOW_LINKS); }
        catch (IOException exception) { return Path.of("__missing_root__").toAbsolutePath(); }
    }

    public static List<String> readSummary(Entry entry, List<Root> roots) throws IOException {
        if (validateFor(entry, roots, Action.VIEW_SUMMARY) != State.AVAILABLE) throw new IOException("Report is unavailable or unsafe");
        if (Files.size(entry.path()) > MAX_SUMMARY_FILE_BYTES) throw new IOException("Report summary exceeds size limit");
        List<String> result = new ArrayList<>();
        int bytes = 0;
        try (var lines = Files.lines(entry.path(), StandardCharsets.UTF_8)) {
            for (String line : lines.limit(MAX_SUMMARY_LINES).toList()) {
                bytes += line.getBytes(StandardCharsets.UTF_8).length;
                if (bytes > MAX_SUMMARY_BYTES) break;
                result.add(line);
            }
        }
        return List.copyOf(result);
    }

    private static Catalog staleOrError(Map<Kind, List<Entry>> entries, Map<Kind, State> states, String error) {
        return new Catalog(Instant.now(), entries, states, !entries.isEmpty(), error);
    }

    private static boolean hasTraversal(Path relative) {
        for (Path part : relative) if ("..".equals(part.toString()) || ".".equals(part.toString())) return true;
        return false;
    }
    private static boolean matches(Root root, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        boolean prefix = root.prefixes().isEmpty() || root.prefixes().stream().anyMatch(value -> lower.startsWith(value.toLowerCase(Locale.ROOT)));
        String extension = extension(name);
        if (root.kind() == Kind.GENERAL && (lower.startsWith("gradlemc-fps-test-") || lower.startsWith("gradlemc-perf-test-")
                || lower.startsWith("gradlemc-worldgen-observation-"))) return false;
        return prefix && root.extensions().contains(extension);
    }
    private static boolean sizeOverSummaryLimit(Path path) {
        try { return Files.size(path) > MAX_SUMMARY_FILE_BYTES; }
        catch (IOException exception) { return true; }
    }
    private static boolean textExtension(String name) { return TEXT_EXTENSIONS.contains(extension(name)); }
    private static String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index).toLowerCase(Locale.ROOT);
    }

    public static List<Root> standardRoots(Path reports, Path exports, Path profiles, Path bundles) {
        return List.of(
                new Root(Kind.FPS, reports, Set.of("gradlemc-fps-test-"), TEXT_EXTENSIONS),
                new Root(Kind.PERFORMANCE, reports, Set.of("gradlemc-perf-test-"), TEXT_EXTENSIONS),
                new Root(Kind.WORLDGEN, reports, Set.of("gradlemc-worldgen-observation-"), TEXT_EXTENSIONS),
                new Root(Kind.GENERAL, reports, Set.of("gradlemc-"), TEXT_EXTENSIONS),
                new Root(Kind.EXPORT, exports, Set.of("gradlemc-"), TEXT_EXTENSIONS),
                new Root(Kind.PROFILER, profiles, Set.of("gradlemc-profile-"), TEXT_EXTENSIONS),
                new Root(Kind.ISSUE_BUNDLE, bundles, Set.of("gradlemc-issue-bundle-"), ARCHIVE_EXTENSIONS));
    }
}

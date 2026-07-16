package com.soumyajit.gradlemc.experiment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Bounded, local properties store. It accepts only sanitized experiment names and a managed root. */
public final class ExperimentStore {
    public static final int MAX_EXPERIMENTS = 16;
    public static final long MAX_FILE_BYTES = 256 * 1024;
    private final Path root;

    public ExperimentStore(Path root) { this.root = root.toAbsolutePath().normalize(); }

    public synchronized ExperimentRecord create(String name, String workflow) throws IOException {
        validateName(name);
        if (workflow == null || !workflow.matches("[a-zA-Z0-9_.:-]{1,96}")) throw new IllegalArgumentException("Invalid workflow ID");
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root)) throw new IOException("Experiment directory cannot be a symbolic link");
        Path path = path(name);
        if (Files.exists(path)) throw new FileAlreadyExistsException(name);
        if (list().size() >= MAX_EXPERIMENTS) throw new IOException("Experiment limit reached; cancel or remove an older experiment explicitly");
        ExperimentRecord record = new ExperimentRecord(name, workflow, Instant.now(), false, null, null, null, null);
        write(record); return record;
    }

    public synchronized Optional<ExperimentRecord> get(String name) throws IOException {
        validateName(name); Path path = path(name); if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        if (Files.size(path) > MAX_FILE_BYTES) throw new IOException("Experiment record exceeds the safe read limit");
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
        return Optional.of(decode(properties));
    }

    public synchronized List<ExperimentRecord> list() throws IOException {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return List.of();
        try (var paths = Files.list(root)) {
            List<ExperimentRecord> records = new ArrayList<>();
            for (Path path : paths.filter(value -> value.getFileName().toString().endsWith(".properties"))
                    .filter(value -> !Files.isSymbolicLink(value)).sorted().limit(MAX_EXPERIMENTS + 1L).toList()) {
                if (records.size() == MAX_EXPERIMENTS || Files.size(path) > MAX_FILE_BYTES) continue;
                Properties properties = new Properties();
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
                records.add(decode(properties));
            }
            return records.stream().sorted(Comparator.comparing(ExperimentRecord::createdAt).reversed()).toList();
        }
    }

    public synchronized void write(ExperimentRecord record) throws IOException {
        validateName(record.name()); Files.createDirectories(root);
        if (Files.isSymbolicLink(root)) throw new IOException("Experiment directory cannot be a symbolic link");
        Properties properties = encode(record);
        StringWriter value = new StringWriter(); properties.store(value, "GradleMC controlled experiment; local and privacy-safe");
        byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FILE_BYTES) throw new IOException("Experiment record exceeds the safe write limit");
        Path target = path(record.name()), temporary = Files.createTempFile(root, ".experiment-", ".tmp");
        try {
            Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private Path path(String name) {
        Path path = root.resolve(name + ".properties").normalize();
        if (!path.getParent().equals(root)) throw new IllegalArgumentException("Experiment path escaped its managed root");
        return path;
    }
    private static void validateName(String name) { if (!ExperimentRecord.validName(name)) throw new IllegalArgumentException("Experiment names must match [a-z0-9][a-z0-9_-]{0,31}"); }

    private static Properties encode(ExperimentRecord record) {
        Properties out = new Properties(); out.setProperty("schema", "1"); out.setProperty("name", record.name()); out.setProperty("workflow", record.workflow());
        out.setProperty("createdAt", record.createdAt().toString()); out.setProperty("cancelled", Boolean.toString(record.cancelled()));
        out.setProperty("manualAction", record.manualAction()); out.setProperty("rollback", record.rollback());
        encodeSnapshot(out, "baseline.", record.baseline()); encodeSnapshot(out, "candidate.", record.candidate()); return out;
    }
    private static void encodeSnapshot(Properties out, String prefix, ExperimentSnapshot snapshot) {
        if (snapshot == null) return; out.setProperty(prefix + "capturedAt", snapshot.capturedAt().toString()); out.setProperty(prefix + "fingerprint", snapshot.fingerprint());
        snapshot.context().forEach((key, value) -> out.setProperty(prefix + "context." + key, value));
        snapshot.metrics().forEach((key, value) -> { if (Double.isFinite(value)) out.setProperty(prefix + "metric." + key, Double.toString(value)); });
    }
    private static ExperimentRecord decode(Properties in) throws IOException {
        if (!"1".equals(in.getProperty("schema"))) throw new IOException("Unsupported experiment schema");
        try { return new ExperimentRecord(in.getProperty("name"), in.getProperty("workflow"), Instant.parse(in.getProperty("createdAt")),
                Boolean.parseBoolean(in.getProperty("cancelled")), decodeSnapshot(in, "baseline."), decodeSnapshot(in, "candidate."),
                in.getProperty("manualAction"), in.getProperty("rollback")); }
        catch (RuntimeException exception) { throw new IOException("Malformed experiment record", exception); }
    }
    private static ExperimentSnapshot decodeSnapshot(Properties in, String prefix) {
        String captured = in.getProperty(prefix + "capturedAt"); if (captured == null) return null;
        Map<String,String> context = new TreeMap<>(); Map<String,Double> metrics = new TreeMap<>();
        for (String key : in.stringPropertyNames()) {
            if (key.startsWith(prefix + "context.")) context.put(key.substring((prefix + "context.").length()), in.getProperty(key));
            else if (key.startsWith(prefix + "metric.")) { double value=Double.parseDouble(in.getProperty(key)); if (Double.isFinite(value)) metrics.put(key.substring((prefix + "metric.").length()), value); }
        }
        return new ExperimentSnapshot(Instant.parse(captured), in.getProperty(prefix + "fingerprint", ""), context, metrics);
    }
}

package com.soumyajit.gradlemc.task;

import com.soumyajit.gradlemc.health.HealthGateResult;
import com.soumyajit.gradlemc.health.HealthGateService;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.ManagedPathSafety;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Versioned, deterministic-in-order, bounded local GradleMC Scan writer. */
public final class GradleMcScanWriter {
    public static final int SCHEMA_VERSION = 3;
    public static final int MAX_SCAN_BYTES = 16 * 1024 * 1024;
    private static final int MAX_FIELD_CHARS = 4096;

    public record ScanFiles(Path text, Path json) { }

    public ScanFiles writeOutcomes(String requested, Map<String, TaskOutcome> outcomes) throws IOException {
        Instant now = Instant.now();
        List<TaskResult> results = new ArrayList<>();
        outcomes.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> results.add(new TaskResult(
                entry.getKey(), entry.getValue().state(), entry.getValue().reason(), entry.getValue().message(), now, now,
                "", false, entry.getValue().outputs(), Map.of(), TaskOverhead.empty())));
        return write(requested, results);
    }

    public ScanFiles write(String requested, List<TaskResult> sourceResults) throws IOException {
        String safeRequested = bounded(requested);
        String id = "scan-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path directory = GradleMcPaths.reportDirectory().toAbsolutePath().normalize();
        ManagedPathSafety.ensureDirectory(GradleMcPaths.gameDirectory(), directory);
        Path text = directory.resolve("gradlemc-" + id + ".txt");
        Path json = directory.resolve("gradlemc-" + id + ".json");
        List<TaskResult> results = sourceResults.stream().sorted(Comparator.comparing(TaskResult::taskId)).toList();
        List<HealthGateResult> gates = HealthGateService.evaluateResults(results);
        String textContent = text(id, safeRequested, results, gates);
        String jsonContent = json(id, safeRequested, results, gates);
        atomicWrite(text, boundedDocument(textContent));
        try {
            atomicWrite(json, boundedDocument(jsonContent));
        } catch (IOException exception) {
            Files.deleteIfExists(text);
            throw exception;
        }
        return new ScanFiles(text, json);
    }

    private static String text(String id, String requested, List<TaskResult> results, List<HealthGateResult> gates) {
        StringBuilder out = new StringBuilder();
        out.append("GradleMC Scan\nSchema: ").append(SCHEMA_VERSION).append("\nScan ID: ").append(id)
                .append("\nRequested: ").append(requested).append("\nLocal-only; no telemetry or upload.\n\nTasks:\n");
        for (TaskResult result : results) {
            out.append(result.taskId()).append(" | ").append(result.state()).append(" | ")
                    .append(bounded(result.reason())).append(" | ").append(bounded(result.message()))
                    .append(" | wallMs=").append(nanosToMillis(result.overhead().wallNanos()))
                    .append(" | outputBytes=").append(result.overhead().outputBytes());
            if (result.overhead().truncated()) out.append(" | TRUNCATED: ").append(result.overhead().truncationReason());
            out.append('\n');
        }
        out.append("\nHealth gates:\n");
        for (HealthGateResult gate : gates) out.append(gate.gateId()).append(" | ").append(gate.state()).append(" | ").append(gate.explanation()).append('\n');
        TaskOverhead total = total(results);
        out.append("\nGradleMC execution overhead (best effort, not CPU accounting):\n")
                .append("wallMs: ").append(nanosToMillis(total.wallNanos())).append('\n')
                .append("gameThreadMs: ").append(nanosToMillis(total.gameThreadNanos())).append('\n')
                .append("workerThreadMs: ").append(nanosToMillis(total.workerThreadNanos())).append('\n')
                .append("filesInspected: ").append(total.filesInspected()).append('\n')
                .append("bytesRead: ").append(total.bytesRead()).append('\n')
                .append("recordsProduced: ").append(total.recordsProduced()).append('\n')
                .append("outputBytes: ").append(total.outputBytes()).append('\n')
                .append("\nLimitations:\n- Runtime evidence is never reused as up-to-date.\n- Correlation is not causation.\n")
                .append("- Game-thread time is wall time for sequential command-thread tasks; it is not process CPU time.\n")
                .append("- Absolute paths and configuration contents are not exported by this writer.\n");
        return out.toString();
    }

    private static String json(String id, String requested, List<TaskResult> results, List<HealthGateResult> gates) {
        StringBuilder out = new StringBuilder(4096);
        out.append("{\n  \"schemaVersion\":").append(SCHEMA_VERSION)
                .append(",\n  \"scanId\":\"").append(escape(id)).append("\"")
                .append(",\n  \"requested\":\"").append(escape(requested)).append("\"")
                .append(",\n  \"privacy\":{\"localOnly\":true,\"telemetry\":false,\"absolutePathsExported\":false}")
                .append(",\n  \"tasks\":[");
        for (int i = 0; i < results.size(); i++) {
            TaskResult result = results.get(i);
            if (i > 0) out.append(',');
            out.append("\n    {\"id\":\"").append(escape(result.taskId())).append("\",\"state\":\"").append(result.state())
                    .append("\",\"reason\":\"").append(escape(bounded(result.reason()))).append("\",\"message\":\"")
                    .append(escape(bounded(result.message()))).append("\",\"fingerprint\":\"").append(escape(result.fingerprint()))
                    .append("\",\"reused\":").append(result.reused()).append(",\"outputs\":");
            appendMap(out, result.outputs());
            out.append(",\"changedInputs\":"); appendMap(out, result.changedInputs());
            out.append(",\"overhead\":"); appendOverhead(out, result.overhead());
            out.append('}');
        }
        out.append("\n  ],\n  \"healthGates\":[");
        for (int i = 0; i < gates.size(); i++) {
            HealthGateResult gate = gates.get(i);
            if (i > 0) out.append(',');
            out.append("\n    {\"id\":\"").append(escape(gate.gateId())).append("\",\"state\":\"").append(gate.state())
                    .append("\",\"explanation\":\"").append(escape(bounded(gate.explanation()))).append("\",\"observed\":");
            if (gate.observedValue() == null || !Double.isFinite(gate.observedValue())) out.append("null"); else out.append(gate.observedValue());
            out.append(",\"threshold\":").append(Double.isFinite(gate.threshold()) ? gate.threshold() : "null").append('}');
        }
        out.append("\n  ],\n  \"overheadSummary\":"); appendOverhead(out, total(results));
        out.append(",\n  \"limitations\":[\"Runtime evidence is never reused as up-to-date\",\"Correlation is not causation\",\"Best-effort overhead is not process CPU accounting\"]\n}\n");
        return out.toString();
    }

    private static void appendMap(StringBuilder out, Map<String, String> map) {
        out.append('{');
        int index = 0;
        for (Map.Entry<String, String> entry : new TreeMap<>(map).entrySet()) {
            if (index++ > 0) out.append(',');
            out.append('\"').append(escape(bounded(entry.getKey()))).append("\":\"").append(escape(bounded(entry.getValue()))).append('\"');
        }
        out.append('}');
    }

    private static void appendOverhead(StringBuilder out, TaskOverhead value) {
        out.append("{\"wallNanos\":").append(value.wallNanos()).append(",\"gameThreadNanos\":").append(value.gameThreadNanos())
                .append(",\"workerThreadNanos\":").append(value.workerThreadNanos()).append(",\"filesInspected\":").append(value.filesInspected())
                .append(",\"bytesRead\":").append(value.bytesRead()).append(",\"samplesCollected\":").append(value.samplesCollected())
                .append(",\"recordsProduced\":").append(value.recordsProduced()).append(",\"estimatedRetainedBytes\":").append(value.estimatedRetainedBytes())
                .append(",\"outputBytes\":").append(value.outputBytes()).append(",\"cancellationLatencyMillis\":").append(value.cancellationLatencyMillis())
                .append(",\"timedOut\":").append(value.timedOut()).append(",\"truncated\":").append(value.truncated())
                .append(",\"truncationReason\":\"").append(escape(bounded(value.truncationReason()))).append("\"}");
    }

    private static TaskOverhead total(List<TaskResult> results) {
        long wall=0, game=0, worker=0, files=0, bytes=0, samples=0, records=0, retained=0, output=0, cancel=0;
        boolean timeout=false, truncated=false; List<String> reasons = new ArrayList<>();
        for (TaskResult result : results) {
            TaskOverhead o = result.overhead(); wall=saturating(wall,o.wallNanos()); game=saturating(game,o.gameThreadNanos()); worker=saturating(worker,o.workerThreadNanos());
            files=saturating(files,o.filesInspected()); bytes=saturating(bytes,o.bytesRead()); samples=saturating(samples,o.samplesCollected()); records=saturating(records,o.recordsProduced());
            retained=saturating(retained,o.estimatedRetainedBytes()); output=saturating(output,o.outputBytes()); cancel=Math.max(cancel,o.cancellationLatencyMillis());
            timeout |= o.timedOut(); truncated |= o.truncated(); if (o.truncated() && !o.truncationReason().isBlank()) reasons.add(o.truncationReason());
        }
        return new TaskOverhead(wall,game,worker,files,bytes,samples,records,retained,output,cancel,timeout,truncated,String.join("; ",new TreeSet<>(reasons)));
    }

    private static long saturating(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    private static long nanosToMillis(long nanos) { return nanos / 1_000_000L; }
    private static String bounded(String value) {
        String safe = redact(value);
        return safe.length() <= MAX_FIELD_CHARS ? safe : safe.substring(0, MAX_FIELD_CHARS) + "…[truncated]";
    }
    private static String redact(String value) {
        if (value == null) return "";
        String safe = replacePath(value, GradleMcPaths.gameDirectory(), "[game-dir]");
        String home = System.getProperty("user.home", "");
        if (!home.isBlank()) safe = replaceLiteral(safe, Path.of(home).toAbsolutePath().normalize().toString(), "[user-home]");
        safe = safe.replaceAll("(?i)(token|password|api[_-]?key|secret)\\s*[=:]\\s*[^\\s,;]+", "$1=[redacted]");
        return safe.replaceAll("(?i)\\b[A-Z]:[\\\\/][^\\s\\\"']+", "[absolute-path]");
    }
    private static String replacePath(String value, Path path, String replacement) {
        try { return replaceLiteral(value, path.toAbsolutePath().normalize().toString(), replacement); }
        catch (RuntimeException ignored) { return value; }
    }
    private static String replaceLiteral(String value, String literal, String replacement) {
        if (literal.isBlank()) return value;
        return java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(literal), java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value).replaceAll(java.util.regex.Matcher.quoteReplacement(replacement));
    }
    private static String boundedDocument(String value) throws IOException { if (value.getBytes(StandardCharsets.UTF_8).length > MAX_SCAN_BYTES) throw new IOException("GradleMC Scan exceeds the configured hard size limit"); return value; }

    private static void atomicWrite(Path target, String value) throws IOException {
        if (Files.exists(target)) throw new FileAlreadyExistsException(target.getFileName().toString());
        Path temporary = Files.createTempFile(target.getParent(), ".gradlemc-scan-", ".tmp");
        try {
            Files.writeString(temporary, value, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, target); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\"); case '"' -> out.append("\\\""); case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format(Locale.ROOT, "\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.toString();
    }
}

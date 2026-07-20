package com.soumyajit.gradlemc.task;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.soumyajit.gradlemc.util.AtomicFiles;
import com.soumyajit.gradlemc.util.GradleMcLimits;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lightweight, bounded report index. A corrupt index is ignored rather than used to delete any report artifacts. */
public final class WorkflowHistoryIndex {
    private static final int MAX_ENTRIES = GradleMcLimits.MAX_REPORT_INDEX_ENTRIES;
    private WorkflowHistoryIndex() { }

    public static synchronized List<Entry> load(Path directory) {
        Path file = directory.resolve("workflow-index.json");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) return recover(directory);
        try {
            if (Files.size(file) > GradleMcLimits.MAX_REPORT_BYTES) return recover(directory);
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray entries = root.getAsJsonArray("entries"); if (entries == null) return List.of();
            List<Entry> result = new ArrayList<>();
            for (JsonElement element : entries) {
                JsonObject item = element.getAsJsonObject();
                Entry entry = new Entry(string(item, "reportId"), string(item, "workflow"), Instant.parse(string(item, "timestamp")), string(item, "state"), item.get("durationNanos").getAsLong(), string(item, "text"), string(item, "json"), string(item, "schemaVersion"));
                if (entry.safe() && entry.filesPresent(directory)) result.add(entry);
            }
            return result.stream().sorted(Comparator.comparing(Entry::timestamp).reversed()).limit(MAX_ENTRIES).toList();
        } catch (IOException | RuntimeException ignored) { quarantine(file); return recover(directory); }
    }

    public static synchronized void record(Path directory, WorkflowResult result, WorkflowReportArtifact artifact) throws IOException {
        List<Entry> entries = new ArrayList<>(load(directory));
        entries.removeIf(entry -> entry.reportId().equals(artifact.reportId()));
        entries.add(new Entry(artifact.reportId(), result.plan().workflowId(), result.endedAt(), result.state().name(), result.elapsedNanos(),
                directory.relativize(artifact.textPath()).toString().replace('\\', '/'), directory.relativize(artifact.jsonPath()).toString().replace('\\', '/'), DiagnosticSchemas.HISTORY_INDEX));
        entries.sort(Comparator.comparing(Entry::timestamp).reversed()); if (entries.size() > MAX_ENTRIES) entries = new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        JsonObject root = new JsonObject(); root.addProperty("schemaVersion", DiagnosticSchemas.HISTORY_INDEX); JsonArray values = new JsonArray();
        for (Entry entry : entries) { JsonObject item = new JsonObject(); item.addProperty("reportId", entry.reportId()); item.addProperty("workflow", entry.workflow()); item.addProperty("timestamp", entry.timestamp().toString()); item.addProperty("state", entry.state()); item.addProperty("durationNanos", entry.durationNanos()); item.addProperty("text", entry.text()); item.addProperty("json", entry.json()); item.addProperty("schemaVersion", entry.schemaVersion()); values.add(item); }
        root.add("entries", values); Files.createDirectories(directory);
        AtomicFiles.writeUtf8(directory.resolve("workflow-index.json"), new Gson().toJson(root));
    }

    private static String string(JsonObject object, String key) { return object.has(key) ? object.get(key).getAsString() : ""; }
    public record Entry(String reportId, String workflow, Instant timestamp, String state, long durationNanos, String text, String json, String schemaVersion) {
        public boolean filesPresent(Path directory) { return safeRelative(text) && safeRelative(json) && Files.isRegularFile(directory.resolve(text), LinkOption.NOFOLLOW_LINKS) && Files.isRegularFile(directory.resolve(json), LinkOption.NOFOLLOW_LINKS); }
        private boolean safe() { return reportId != null && reportId.matches("workflow-[A-Za-z0-9._-]{1,96}") && workflow != null && workflow.length() <= 128 && durationNanos >= 0L && safeRelative(text) && safeRelative(json); }
    }

    private static boolean safeRelative(String value) { return value != null && value.matches("workflow-[A-Za-z0-9._-]{1,96}\\.(?:txt|json)") && !value.contains("..") && !value.contains("\\"); }
    private static List<Entry> recover(Path directory) {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) return List.of();
        try (var paths = Files.list(directory)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).filter(path -> !Files.isSymbolicLink(path))
                    .map(path -> path.getFileName().toString()).filter(name -> name.matches("workflow-[A-Za-z0-9._-]{1,96}\\.txt"))
                    .filter(name -> Files.isRegularFile(directory.resolve(name.substring(0, name.length() - 4) + ".json"), LinkOption.NOFOLLOW_LINKS))
                    .sorted().limit(MAX_ENTRIES)
                    .map(name -> new Entry(name.substring(0, name.length() - 4), "recovered", Instant.EPOCH, "UNKNOWN", 0L, name, name.substring(0, name.length() - 4) + ".json", DiagnosticSchemas.HISTORY_INDEX)).toList();
        } catch (IOException ignored) { return List.of(); }
    }
    private static void quarantine(Path file) { try { Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt")); } catch (IOException ignored) { } }
}

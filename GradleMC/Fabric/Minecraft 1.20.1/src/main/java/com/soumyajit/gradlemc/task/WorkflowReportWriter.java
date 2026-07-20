package com.soumyajit.gradlemc.task;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.util.AtomicFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;

/** Serializes one immutable final result into matching TXT and JSON; collectors are never re-run while writing. */
public final class WorkflowReportWriter {
    public WorkflowReportArtifact write(WorkflowResult result, Path directory) throws IOException {
        if (result == null) throw new IllegalArgumentException("result is required");
        String id = "workflow-" + result.runId();
        if (!id.matches("workflow-[A-Za-z0-9._-]{1,96}")) throw new IOException("Invalid workflow report ID");
        Path text = directory.resolve(id + ".txt"); Path json = directory.resolve(id + ".json");
        AtomicFiles.writeUtf8(json, new GsonBuilder().setPrettyPrinting().create().toJson(json(result)));
        AtomicFiles.writeUtf8(text, text(result));
        return new WorkflowReportArtifact(id, text, json);
    }

    private static String text(WorkflowResult result) {
        StringBuilder out = new StringBuilder("GradleMC Diagnostic Workflow Report\n==================================\n");
        out.append("Report ID: ").append(result.runId()).append('\n').append("GradleMC: ").append(GradleMC.version()).append("; ").append(GradleMC.CURRENT_LOADER_NAME).append(' ').append(GradleMC.CURRENT_MINECRAFT_VERSION).append('\n');
        out.append("Workflow: ").append(result.plan().workflowId()).append('\n').append("State: ").append(result.state()).append('\n').append("Started: ").append(result.startedAt()).append('\n').append("Ended: ").append(result.endedAt()).append('\n');
        out.append("Elapsed: ").append(Duration.ofNanos(result.elapsedNanos()).toMillis()).append(" ms\n");
        out.append("Evidence schema: ").append(DiagnosticSchemas.EVIDENCE)
                .append("\nProvenance: ").append(result.provenance()).append("\n\nPlan\n----\n");
        result.plan().orderedTasks().forEach(task -> out.append("- ").append(task.id()).append(" [").append(task.environment()).append("]\n"));
        out.append("\nTask results\n------------\n");
        result.taskResults().forEach(task -> {
            out.append("- ").append(task.taskId()).append(": ").append(task.state()).append(" (").append(Duration.ofNanos(task.elapsedNanos()).toMillis()).append(" ms)");
            if (!task.missingPrerequisite().isBlank()) out.append("; unavailable: ").append(task.missingPrerequisite());
            if (!task.cancellationReason().isBlank()) out.append("; cancellation: ").append(task.cancellationReason());
            out.append('\n');
            task.evidence().stream().sorted(Comparator.comparing(DiagnosticEvidence::id)).forEach(evidence -> out
                    .append("  observation ").append(evidence.id()).append(" [").append(evidence.availability()).append("] ")
                    .append(formatValues(evidence.values())).append(' ').append(evidence.unit()).append("\n"));
            task.warnings().forEach(warning -> out.append("  warning: ").append(warning).append('\n'));
            task.errors().forEach(error -> out.append("  error: ").append(error).append('\n'));
        });
        if (!result.cancellationReason().isBlank()) out.append("\nCancellation/timeout: ").append(result.cancellationReason()).append('\n');
        return out.toString();
    }

    private static String formatValues(Map<String, String> values) {
        return values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(", ", "{", "}"));
    }

    private static JsonObject json(WorkflowResult result) {
        JsonObject root = new JsonObject(); root.addProperty("schemaVersion", DiagnosticSchemas.REPORT); root.addProperty("reportId", result.runId());
        JsonObject environment = new JsonObject(); environment.addProperty("gradleMcVersion", GradleMC.version()); environment.addProperty("loader", GradleMC.CURRENT_LOADER_NAME); environment.addProperty("minecraft", GradleMC.CURRENT_MINECRAFT_VERSION); environment.addProperty("java", System.getProperty("java.version", "unknown")); root.add("environment", environment);
        root.addProperty("workflowId", result.plan().workflowId()); root.addProperty("state", result.state().name()); root.addProperty("startedAt", result.startedAt().toString()); root.addProperty("endedAt", result.endedAt().toString()); root.addProperty("elapsedNanos", result.elapsedNanos()); root.addProperty("provenance", result.provenance()); root.addProperty("cancellationReason", result.cancellationReason());
        JsonArray plan = new JsonArray(); result.plan().orderedTasks().forEach(task -> { JsonObject value = new JsonObject(); value.addProperty("id", task.id()); value.addProperty("environment", task.environment().name()); value.addProperty("timeoutMillis", task.timeout().toMillis()); plan.add(value); }); root.add("plan", plan);
        JsonArray tasks = new JsonArray(); result.taskResults().forEach(task -> tasks.add(taskJson(task))); root.add("tasks", tasks); return root;
    }

    private static JsonObject taskJson(TaskResult task) {
        JsonObject value = new JsonObject(); value.addProperty("id", task.taskId()); value.addProperty("state", task.state().name()); value.addProperty("startedAt", task.startedAt().toString()); value.addProperty("endedAt", task.endedAt().toString()); value.addProperty("elapsedNanos", task.elapsedNanos()); value.addProperty("environment", task.environment().name()); value.addProperty("missingPrerequisite", task.missingPrerequisite()); value.addProperty("cancellationReason", task.cancellationReason()); value.addProperty("provenance", task.provenance());
        JsonArray evidence = new JsonArray(); task.evidence().stream().sorted(Comparator.comparing(DiagnosticEvidence::id)).forEach(item -> { JsonObject itemJson = new JsonObject(); itemJson.addProperty("id", item.id()); itemJson.addProperty("type", item.type().name()); itemJson.addProperty("sourceTaskId", item.sourceTaskId()); itemJson.addProperty("availability", item.availability().name()); itemJson.addProperty("unit", item.unit()); itemJson.addProperty("scope", item.scope()); itemJson.addProperty("confidence", item.confidence()); itemJson.addProperty("limitation", item.limitation()); JsonObject fields = new JsonObject(); item.values().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> fields.addProperty(entry.getKey(), entry.getValue())); itemJson.add("values", fields); evidence.add(itemJson); }); value.add("evidence", evidence);
        JsonArray warnings = new JsonArray(); task.warnings().forEach(warnings::add); value.add("warnings", warnings); JsonArray errors = new JsonArray(); task.errors().forEach(errors::add); value.add("errors", errors); return value;
    }

}

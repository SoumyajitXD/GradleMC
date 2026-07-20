package com.soumyajit.gradlemc.task;

import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class WorkflowReportWriterSelfTest {
    private WorkflowReportWriterSelfTest() { }

    public static void run() {
        Path directory = Path.of("build", "gradlemc-selftest-workflow-report").toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
            DiagnosticTask task = new DiagnosticTask("runtime", "Runtime", "", "environment", TaskEnvironment.ANY, List.of(), java.time.Duration.ofSeconds(1), true, List.of(EvidenceType.ENVIRONMENT), context -> TaskOutcome.success(List.of()));
            WorkflowPlan plan = new WorkflowPlan("quick", Instant.parse("2026-01-01T00:00:00Z"), List.of(task), Map.of(), List.of(EvidenceType.ENVIRONMENT), 1_000_000_000L);
            DiagnosticEvidence evidence = new DiagnosticEvidence("environment.runtime", EvidenceType.ENVIRONMENT, "runtime", Instant.parse("2026-01-01T00:00:01Z"), "process", Map.of("loader", "Fabric"), "", EvidenceAvailability.AVAILABLE, "high", "snapshot", "test");
            TaskResult taskResult = new TaskResult("runtime", TaskState.SUCCEEDED, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:01Z"), 1_000_000_000L, List.of(evidence), List.of(), List.of(), "", "", TaskEnvironment.ANY, "test");
            WorkflowResult result = new WorkflowResult("selftest", plan, TaskState.SUCCEEDED, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:01Z"), 1_000_000_000L, List.of(taskResult), "", "test");
            WorkflowReportArtifact artifact = new WorkflowReportWriter().write(result, directory);
            require(Files.isRegularFile(artifact.textPath()) && Files.isRegularFile(artifact.jsonPath()), "both artifacts must be written");
            require(JsonParser.parseString(Files.readString(artifact.jsonPath())).getAsJsonObject().get("reportId").getAsString().equals("selftest"), "JSON must be valid and derived from the same result");
            WorkflowHistoryIndex.record(directory, result, artifact);
            require(WorkflowHistoryIndex.load(directory).size() == 1, "history index must retain lightweight report metadata");
            Files.writeString(directory.resolve("workflow-index.json"), "not json");
            require(WorkflowHistoryIndex.load(directory).size() == 1, "corrupt history index must recover report metadata without deleting report files");
            Files.deleteIfExists(artifact.textPath()); Files.deleteIfExists(artifact.jsonPath()); Files.deleteIfExists(directory.resolve("workflow-index.json")); Files.deleteIfExists(directory.resolve("workflow-index.json.corrupt")); Files.deleteIfExists(directory);
        } catch (Exception exception) { throw new AssertionError("workflow report writer self-test failed", exception); }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

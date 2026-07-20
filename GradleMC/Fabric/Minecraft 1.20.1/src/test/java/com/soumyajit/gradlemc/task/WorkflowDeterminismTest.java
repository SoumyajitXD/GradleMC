package com.soumyajit.gradlemc.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowDeterminismTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void equivalentEvidenceMapsProduceByteIdenticalTextAndJsonReports() throws Exception {
        WorkflowReportWriter writer = new WorkflowReportWriter();
        WorkflowReportArtifact first = writer.write(result(Map.of("zeta", "last", "alpha", "first")), temporaryDirectory.resolve("first"));
        WorkflowReportArtifact second = writer.write(result(reverseInsertion()), temporaryDirectory.resolve("second"));

        assertEquals(Files.readString(first.textPath()), Files.readString(second.textPath()));
        assertEquals(Files.readString(first.jsonPath()), Files.readString(second.jsonPath()));
    }

    @Test
    void recoveredHistoryOrderingDoesNotDependOnFilesystemEnumeration() throws Exception {
        for (String id : List.of("workflow-z", "workflow-a", "workflow-m")) {
            Files.writeString(temporaryDirectory.resolve(id + ".txt"), "text");
            Files.writeString(temporaryDirectory.resolve(id + ".json"), "{}");
        }
        assertEquals(List.of("workflow-a", "workflow-m", "workflow-z"),
                WorkflowHistoryIndex.load(temporaryDirectory).stream().map(WorkflowHistoryIndex.Entry::reportId).toList());
    }

    @Test
    void rejectedReportSubmissionFailsTruthfullyBeforeSuccessIsAnnounced() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> FabricDiagnosticService.submitReportTask(command -> {
                    throw new java.util.concurrent.RejectedExecutionException("closed");
                }, () -> "unreachable"));

        assertTrue(failure.getMessage().contains("not started"));
        assertInstanceOf(java.util.concurrent.RejectedExecutionException.class, failure.getCause());
    }

    private static Map<String, String> reverseInsertion() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("alpha", "first");
        values.put("zeta", "last");
        return values;
    }

    private static WorkflowResult result(Map<String, String> values) {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        DiagnosticTask task = new DiagnosticTask("runtime", "Runtime", "", "test", TaskEnvironment.ANY, List.of(), Duration.ofSeconds(1), true, List.of(EvidenceType.ENVIRONMENT), context -> TaskOutcome.success(List.of()));
        WorkflowPlan plan = new WorkflowPlan("quick", start, List.of(task), Map.of(), List.of(EvidenceType.ENVIRONMENT), 1_000_000_000L);
        DiagnosticEvidence evidence = new DiagnosticEvidence("environment.runtime", EvidenceType.ENVIRONMENT, "runtime", start, "process", values, "", EvidenceAvailability.AVAILABLE, "high", "snapshot", "test");
        TaskResult taskResult = new TaskResult("runtime", TaskState.SUCCEEDED, start, start.plusSeconds(1), 1_000_000_000L, List.of(evidence), List.of(), List.of(), "", "", TaskEnvironment.ANY, "test");
        return new WorkflowResult("same", plan, TaskState.SUCCEEDED, start, start.plusSeconds(1), 1_000_000_000L, List.of(taskResult), "", "test");
    }
}

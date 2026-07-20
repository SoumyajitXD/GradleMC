package com.soumyajit.gradlemc.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportDiscoveryTest {
    @Test
    void discoversWorkflowTextReportsButNotWorkflowJsonOrIndexFiles() {
        assertTrue(GradleMcCommands.isDiscoverableTextReportName("workflow-run-123.txt"));
        assertTrue(GradleMcCommands.isDiscoverableTextReportName("gradlemc-perf-test-123.txt"));
        assertFalse(GradleMcCommands.isDiscoverableTextReportName("workflow-run-123.json"));
        assertFalse(GradleMcCommands.isDiscoverableTextReportName("workflow-index.json"));
        assertFalse(GradleMcCommands.isDiscoverableTextReportName("unrelated.txt"));
    }
}

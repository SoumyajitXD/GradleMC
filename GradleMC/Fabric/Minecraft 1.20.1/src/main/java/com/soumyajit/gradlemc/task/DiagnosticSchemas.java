package com.soumyajit.gradlemc.task;

/** Stable persisted schema and provenance identifiers. Product version changes do not change these values. */
public final class DiagnosticSchemas {
    public static final String SNAPSHOT = "fabric-diagnostic-snapshot-v1";
    public static final String EVIDENCE = "fabric-diagnostic-evidence-v1";
    public static final String REPORT = "fabric-diagnostic-report-v1";
    public static final String HISTORY_INDEX = "workflow-history-index-v1";
    public static final String COLLECTOR = "fabric-diagnostic-service-v1";

    private DiagnosticSchemas() {
    }
}

package com.soumyajit.gradlemc.smart;

/** Evidence used by local Smart Diagnostics rules; this is an interpretation input, not a live collector handle. */
public record DiagnosticEvidence(String id, String metric, String observed, String threshold, String detail,
                                 String sourceTaskId, String unit, String availability, String limitation) {
    public DiagnosticEvidence {
        id = id == null || id.isBlank() ? "smart." + safe(metric).replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase(java.util.Locale.ROOT) : id;
        metric = safe(metric); observed = safe(observed); threshold = safe(threshold); detail = safe(detail);
        sourceTaskId = sourceTaskId == null || sourceTaskId.isBlank() ? "unknown-source" : sourceTaskId;
        unit = unit == null ? "" : unit; availability = availability == null || availability.isBlank() ? "available" : availability;
        limitation = limitation == null ? "" : limitation;
    }
    public static DiagnosticEvidence observed(String metric, String observed, String threshold, String detail) {
        return new DiagnosticEvidence(null, metric, observed, threshold, detail,
                "stability-advisor", "", "available", "Latest bounded local observation.");
    }
    private static String safe(String value) { return value == null ? "" : value; }
}

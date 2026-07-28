package com.soumyajit.gradlemc.report;

/** Regression coverage for the shared persistent-diagnostic redaction policy. */
public final class DiagnosticRedactorSelfTest {
    private DiagnosticRedactorSelfTest() { }

    public static void run() {
        String input = "Authorization: Bearer abc.def\nuri=https://alice:pw@example.invalid/a token=xyz \\\\server\\share\\secret.txt \\\\?\\C:\\private\\x password=hidden\u0001";
        String safe = DiagnosticRedactor.redact(input);
        check(!safe.contains("abc.def") && !safe.contains("alice:pw") && !safe.contains("xyz") && !safe.contains("hidden"), "credential forms are removed");
        check(!safe.contains("server\\share") && !safe.contains("C:\\private") && !safe.contains("\u0001"), "network paths and controls are removed");
        check(safe.contains("[redacted]") && safe.contains("[absolute-path]"), "redaction markers are deterministic");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("Diagnostic redactor self-test failed: " + message);
    }
}

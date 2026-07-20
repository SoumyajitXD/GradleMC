package com.soumyajit.gradlemc.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IssueBundleSafetyTest {
    @Test
    void zipEntryNamesRejectTraversalAbsoluteAndInjectionForms() {
        assertTrue(IssueBundleExporter.safeEntryName("generated/environment-summary.txt"));
        for (String unsafe : new String[]{
                "../secret", "safe/../../secret", "/absolute", "C:/drive", "\\\\server\\share",
                "mixed\\../secret", "line\nbreak.txt", "control\u0001.txt", "", "a".repeat(129)
        }) assertFalse(IssueBundleExporter.safeEntryName(unsafe), unsafe);
    }
}

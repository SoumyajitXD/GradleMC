package com.soumyajit.gradlemc.config;

import com.soumyajit.gradlemc.profiler.ProfilerSessionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticDurationPolicyTest {
    @Test
    void configuredMaximumIsClampedToTheSupportedRange() {
        assertEquals(5, DiagnosticDurationPolicy.boundedMaximum(-20, 5));
        assertEquals(600, DiagnosticDurationPolicy.boundedMaximum(600, 5));
        assertEquals(1_800, DiagnosticDurationPolicy.boundedMaximum(Integer.MAX_VALUE, 5));
    }

    @Test
    void profilerRejectsNonFiniteThresholds() {
        ProfilerSessionConfig sanitized = new ProfilerSessionConfig(60, 20, "server", Double.NaN, false, null).sanitized();
        assertTrue(Double.isFinite(sanitized.onlyTicksOverMillis()));
        assertEquals(ProfilerSessionConfig.MIN_SLOW_TICK_MILLIS, sanitized.onlyTicksOverMillis());
    }
}

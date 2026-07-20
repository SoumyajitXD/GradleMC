package com.soumyajit.gradlemc.smart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdaptiveBaselineTest {
    @Test
    void metricSamplesRemainPositiveWhenConfiguredLimitIsMalformed() {
        AdaptiveBaseline.MetricStats initial = new AdaptiveBaseline.MetricStats(1, 10.0D, 10.0D, 10.0D);
        AdaptiveBaseline.MetricStats next = initial.add(20.0D, -5);
        assertEquals(1, next.samples());
        assertEquals(20.0D, next.average());
    }

    @Test
    void metricStatsRejectNonFinitePersistedValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveBaseline.MetricStats(1, Double.NaN, 0.0D, 0.0D));
    }
}

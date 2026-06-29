package com.soumyajit.gradlemc;

import com.soumyajit.gradlemc.ai.AdaptiveRiskCalculatorSelfTest;
import com.soumyajit.gradlemc.client.overlay.FpsRollingStatsCalculatorSelfTest;
import com.soumyajit.gradlemc.config.OverlayDefaultsSelfTest;
import com.soumyajit.gradlemc.profiler.ProfilerCoreSelfTest;
import com.soumyajit.gradlemc.util.GradleMcPathsSelfTest;

public final class GradleMcSelfTest {
    private GradleMcSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        AdaptiveRiskCalculatorSelfTest.run();
        FpsRollingStatsCalculatorSelfTest.run();
        OverlayDefaultsSelfTest.run();
        GradleMcPathsSelfTest.run();
        ProfilerCoreSelfTest.run();
    }
}

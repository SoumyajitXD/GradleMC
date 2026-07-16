package com.soumyajit.gradlemc;

import com.soumyajit.gradlemc.ai.AdaptiveRiskCalculatorSelfTest;
import com.soumyajit.gradlemc.client.overlay.FpsRollingStatsCalculatorSelfTest;
import com.soumyajit.gradlemc.client.overlay.OverlayLineComposerSelfTest;
import com.soumyajit.gradlemc.metrics.FrameTimeStatisticsSelfTest;
import com.soumyajit.gradlemc.config.OverlayDefaultsSelfTest;
import com.soumyajit.gradlemc.profiler.ProfilerCoreSelfTest;
import com.soumyajit.gradlemc.util.GradleMcPathsSelfTest;
import com.soumyajit.gradlemc.modaudit.ModAuditModelSelfTest;
import com.soumyajit.gradlemc.task.TaskEngineSelfTest;
import com.soumyajit.gradlemc.instance.InstanceModelSelfTest;
import com.soumyajit.gradlemc.health.HealthGateSelfTest;
import com.soumyajit.gradlemc.experiment.ExperimentSelfTest;
import com.soumyajit.gradlemc.incident.IncidentSelfTest;
import com.soumyajit.gradlemc.lock.InstanceLockSelfTest;
import com.soumyajit.gradlemc.client.gui.model.ModListViewModelSelfTest;
import com.soumyajit.gradlemc.client.gui.WorkspaceSupportSelfTest;
import com.soumyajit.gradlemc.foundation.FoundationSelfTest;
import com.soumyajit.gradlemc.scan.GradleMcScanV1SelfTest;
import com.soumyajit.gradlemc.adaptive.AdaptiveDiagnosticsSelfTest;

public final class GradleMcSelfTest {
    private GradleMcSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        AdaptiveRiskCalculatorSelfTest.run();
        FpsRollingStatsCalculatorSelfTest.run();
        OverlayLineComposerSelfTest.run();
        FrameTimeStatisticsSelfTest.run();
        OverlayDefaultsSelfTest.run();
        GradleMcPathsSelfTest.run();
        ProfilerCoreSelfTest.run();
        ModAuditModelSelfTest.run();
        TaskEngineSelfTest.run();
        InstanceModelSelfTest.run();
        HealthGateSelfTest.run();
        ExperimentSelfTest.run();
        IncidentSelfTest.run();
        InstanceLockSelfTest.run();
        ModListViewModelSelfTest.run();
        WorkspaceSupportSelfTest.run();
        FoundationSelfTest.run();
        GradleMcScanV1SelfTest.run();
        AdaptiveDiagnosticsSelfTest.run();
    }
}

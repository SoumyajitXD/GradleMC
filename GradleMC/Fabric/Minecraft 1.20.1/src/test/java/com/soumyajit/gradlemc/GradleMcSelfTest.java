package com.soumyajit.gradlemc;

import com.soumyajit.gradlemc.metrics.FrameTimeStatisticsSelfTest;
import com.soumyajit.gradlemc.client.overlay.FpsRollingStatsCalculatorSelfTest;
import com.soumyajit.gradlemc.client.overlay.FpsSamplingServiceSelfTest;
import com.soumyajit.gradlemc.modaudit.FabricModAuditModelSelfTest;
import com.soumyajit.gradlemc.config.GradleMCConfigSelfTest;
import com.soumyajit.gradlemc.report.FpsTestReportWriterSelfTest;
import com.soumyajit.gradlemc.client.overlay.OverlayLineComposerSelfTest;
import com.soumyajit.gradlemc.task.TaskEngineSelfTest;
import com.soumyajit.gradlemc.task.WorkflowReportWriterSelfTest;
import com.soumyajit.gradlemc.network.ClientRequestTrackerSelfTest;
import com.soumyajit.gradlemc.util.StoragePrivacySelfTest;

public final class GradleMcSelfTest {
    private GradleMcSelfTest() {
    }

    public static void main(String[] args) {
        FrameTimeStatisticsSelfTest.run();
        FpsRollingStatsCalculatorSelfTest.run();
        FpsSamplingServiceSelfTest.run();
        FabricModAuditModelSelfTest.run();
        GradleMCConfigSelfTest.run();
        FpsTestReportWriterSelfTest.run();
        OverlayLineComposerSelfTest.run();
        TaskEngineSelfTest.run();
        WorkflowReportWriterSelfTest.run();
        ClientRequestTrackerSelfTest.run();
        StoragePrivacySelfTest.run();
        System.out.println("GradleMC Fabric self-tests passed.");
    }
}

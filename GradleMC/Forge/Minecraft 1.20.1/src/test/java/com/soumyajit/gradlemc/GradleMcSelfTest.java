package com.soumyajit.gradlemc;

import com.soumyajit.gradlemc.ai.AdaptiveRiskCalculatorSelfTest;
import com.soumyajit.gradlemc.ai.AdaptiveSyncSelfTest;
import com.soumyajit.gradlemc.client.overlay.FpsRollingStatsCalculatorSelfTest;
import com.soumyajit.gradlemc.client.overlay.OverlayLineComposerSelfTest;
import com.soumyajit.gradlemc.metrics.FrameTimeStatisticsSelfTest;
import com.soumyajit.gradlemc.metrics.ServerHealthTelemetrySelfTest;
import com.soumyajit.gradlemc.metrics.TickMonitorServiceSelfTest;
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
import com.soumyajit.gradlemc.client.gui.LegacyGuiLayoutSelfTest;
import com.soumyajit.gradlemc.client.gui.GuiLocalizationSelfTest;
import com.soumyajit.gradlemc.foundation.FoundationSelfTest;
import com.soumyajit.gradlemc.foundation.GameThreadBridgeSelfTest;
import com.soumyajit.gradlemc.scan.GradleMcScanV1SelfTest;
import com.soumyajit.gradlemc.adaptive.AdaptiveDiagnosticsSelfTest;
import com.soumyajit.gradlemc.investigation.InvestigationSelfTest;
import com.soumyajit.gradlemc.investigation.storage.InvestigationStorageSelfTest;
import com.soumyajit.gradlemc.investigation.storage.AtomicUtf8FileSelfTest;
import com.soumyajit.gradlemc.investigation.profile.InvestigationProfileSelfTest;
import com.soumyajit.gradlemc.investigation.planning.InvestigationPlanningSelfTest;
import com.soumyajit.gradlemc.investigation.session.InvestigationSessionServiceSelfTest;
import com.soumyajit.gradlemc.report.DiagnosticRedactorSelfTest;
import com.soumyajit.gradlemc.report.SharedReleaseEvidenceSelfTest;
import com.soumyajit.gradlemc.performance.PerformanceGuardSelfTest;
import com.soumyajit.gradlemc.performance.PerformancePolicySelfTest;
import com.soumyajit.gradlemc.metrics.MeasurementHubSelfTest;
import com.soumyajit.gradlemc.metrics.ServerPerformanceChannelSelfTest;

public final class GradleMcSelfTest {
    private GradleMcSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        AdaptiveRiskCalculatorSelfTest.run();
        AdaptiveSyncSelfTest.run();
        FpsRollingStatsCalculatorSelfTest.run();
        OverlayLineComposerSelfTest.run();
        FrameTimeStatisticsSelfTest.run();
        ServerHealthTelemetrySelfTest.run();
        TickMonitorServiceSelfTest.run();
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
        LegacyGuiLayoutSelfTest.run();
        GuiLocalizationSelfTest.run();
        FoundationSelfTest.run();
        GameThreadBridgeSelfTest.run();
        GradleMcScanV1SelfTest.run();
        AdaptiveDiagnosticsSelfTest.run();
        InvestigationSelfTest.run();
        InvestigationStorageSelfTest.run();
        AtomicUtf8FileSelfTest.run();
        InvestigationProfileSelfTest.run();
        InvestigationPlanningSelfTest.run();
        InvestigationSessionServiceSelfTest.run();
        DiagnosticRedactorSelfTest.run();
        SharedReleaseEvidenceSelfTest.run();
        PerformanceGuardSelfTest.run();
        PerformancePolicySelfTest.run();
        MeasurementHubSelfTest.run();
        ServerPerformanceChannelSelfTest.run();
    }
}

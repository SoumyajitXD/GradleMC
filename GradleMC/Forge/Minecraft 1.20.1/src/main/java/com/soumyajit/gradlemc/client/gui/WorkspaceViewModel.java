package com.soumyajit.gradlemc.client.gui;

import com.soumyajit.gradlemc.client.gui.model.GradleMCGuiState;
import com.soumyajit.gradlemc.modaudit.ModAuditResult;
import com.soumyajit.gradlemc.modaudit.ModAuditService;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;

/** Immutable screen input. Capture happens during init/tick, never while rendering. */
public record WorkspaceViewModel(GradleMCGuiState state, ModAuditResult modAudit, long capturedAtMillis) {
    public static WorkspaceViewModel capture() {
        return new WorkspaceViewModel(GradleMCGuiState.capture(GradleMCGuiBridge.latestSmartAIStatus()),
                ModAuditService.current(), System.currentTimeMillis());
    }
}

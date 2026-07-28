package com.soumyajit.gradlemc.investigation.planning;
public record InvestigationPlanLimitation(String code, String message) {
    public InvestigationPlanLimitation { if (code == null || code.isBlank() || message == null || message.isBlank()) throw new IllegalArgumentException("Plan limitation is required"); }
}

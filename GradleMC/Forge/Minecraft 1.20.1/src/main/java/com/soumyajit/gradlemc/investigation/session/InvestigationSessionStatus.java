package com.soumyajit.gradlemc.investigation.session;

/** Orchestration view; persisted state remains InvestigationState. */
public enum InvestigationSessionStatus {
    CREATED, PLANNED, RUNNING, CANCELLING, COMPLETED, FAILED, CANCELLED;
    public boolean terminal() { return this == COMPLETED || this == FAILED || this == CANCELLED; }
}

package com.soumyajit.gradlemc.investigation;
public enum InvestigationState { CREATED, RUNNING, COMPLETED, COMPLETED_WITH_LIMITATIONS, CANCELLED, FAILED;
    public boolean terminal(){return this==COMPLETED||this==COMPLETED_WITH_LIMITATIONS||this==CANCELLED||this==FAILED;}}

package com.soumyajit.gradlemc.investigation.session;

/** Deliberately small, immutable policy. It never changes the built-in profile catalog. */
public record InvestigationSessionPolicy(boolean allowPartialWhenRequiredRootsViable) {
    public static final InvestigationSessionPolicy SAFE_DEFAULT = new InvestigationSessionPolicy(true);
}

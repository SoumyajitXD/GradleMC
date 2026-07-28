package com.soumyajit.gradlemc.investigation;
public record InvestigationFailure(String code, String message, boolean fatal) { public InvestigationFailure { InvestigationText.code(code); message=InvestigationText.safe(message); } }

package com.soumyajit.gradlemc.investigation;
public record InvestigationLimitation(String code, String message) { public InvestigationLimitation { InvestigationText.code(code); message=InvestigationText.safe(message); } }

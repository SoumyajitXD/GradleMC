package com.soumyajit.gradlemc.investigation;
import com.soumyajit.gradlemc.report.DiagnosticRedactor;
final class InvestigationText { static final int MAX=1024; private InvestigationText(){} static void code(String v){if(v==null||!v.matches("[a-z][a-z0-9-]{0,63}"))throw new IllegalArgumentException("Invalid code");} static String safe(String v){if(v==null)throw new IllegalArgumentException("text is required");String s=DiagnosticRedactor.redact(v);if(s.length()>MAX)throw new IllegalArgumentException("text exceeds limit");return s;} }

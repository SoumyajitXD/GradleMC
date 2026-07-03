package com.soumyajit.gradlemc.profiler.report;

public record ModAttribution(String source, String confidence, long samples, String reason) {
}

package com.soumyajit.gradlemc.task;

import java.nio.file.Path;

public record WorkflowReportArtifact(String reportId, Path textPath, Path jsonPath) { }

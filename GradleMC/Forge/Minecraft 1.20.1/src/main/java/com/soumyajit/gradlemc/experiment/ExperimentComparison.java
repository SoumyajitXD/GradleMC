package com.soumyajit.gradlemc.experiment;

import java.util.List;
import java.util.Map;

public record ExperimentComparison(ExperimentVerdict verdict, String explanation,
                                   Map<String, Double> relativeChanges, List<String> comparabilityIssues) {
    public ExperimentComparison {
        explanation = explanation == null ? "" : explanation;
        relativeChanges = Map.copyOf(relativeChanges);
        comparabilityIssues = List.copyOf(comparabilityIssues);
    }
}

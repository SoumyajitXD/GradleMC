package com.soumyajit.gradlemc.task;

import java.util.List;

/** Bounded, serialisable failure information. Stack traces are intentionally not history payloads. */
public record TaskFailure(String code, String category, String summary, String technicalDetail,
                          String failedTaskId, List<String> dependencyChain,
                          boolean retryMaySucceed, String suggestedNextAction) {
    public TaskFailure {
        code = safe(code); category = safe(category); summary = safe(summary);
        technicalDetail = limit(safe(technicalDetail), 512); failedTaskId = safe(failedTaskId);
        dependencyChain = List.copyOf(dependencyChain == null ? List.of() : dependencyChain);
        suggestedNextAction = safe(suggestedNextAction);
    }
    private static String safe(String value) { return value == null ? "" : value; }
    private static String limit(String value, int maximum) { return value.length() <= maximum ? value : value.substring(0, maximum); }
}

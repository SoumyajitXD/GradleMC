package com.soumyajit.gradlemc.task;

/** Why a task was executed or reused.  Reasons are safe for command and history summaries. */
public record CacheDecision(Kind kind, String explanation, String key) {
    public enum Kind {
        NOT_CACHEABLE, ELIGIBLE, HIT, MISS, DISABLED, MISSING_FINGERPRINT,
        FINGERPRINT_FAILURE, INCOMPATIBLE_VERSION, CORRUPT_ENTRY, OVERSIZED_ENTRY,
        WRITE_SKIPPED, WRITE_FAILED
    }

    public CacheDecision {
        kind = kind == null ? Kind.NOT_CACHEABLE : kind;
        explanation = explanation == null ? "" : explanation;
        key = key == null ? "" : key;
    }

    public static CacheDecision notCacheable(String reason) {
        return new CacheDecision(Kind.NOT_CACHEABLE, reason, "");
    }
}

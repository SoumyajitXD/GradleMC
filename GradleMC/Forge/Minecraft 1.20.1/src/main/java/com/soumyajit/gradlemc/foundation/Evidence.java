package com.soumyajit.gradlemc.foundation;

import java.time.Instant;
import java.util.*;

/** Typed, bounded diagnostic evidence. Classifications deliberately carry different validation rules. */
public record Evidence(String id, Classification classification, String title, String description, TaskId sourceTask,
                       Instant capturedAt, Freshness freshness, Optional<StaticFingerprint> staticFingerprint,
                       Optional<RuntimeContextFingerprint> runtimeFingerprint, Map<String, String> fields,
                       Confidence confidence, List<String> limitations, Optional<Attribution> attribution,
                       List<String> relatedIds, int schemaVersion) {
    public static final int MAX_FIELDS = 32, MAX_RELATIONS = 16, MAX_LIMITATIONS = 8;
    public enum Classification { OBSERVED_FACT, EXACT_ATTRIBUTION, CORRELATION, RULE_MATCH, HEURISTIC, HYPOTHESIS }
    public enum Confidence { LOW, MEDIUM, HIGH, NOT_APPLICABLE }
    public record Attribution(String target, String exactWhy) {
        public Attribution { if (blank(target) || blank(exactWhy)) throw new IllegalArgumentException("Exact attribution requires target and basis"); }
    }
    public Evidence {
        if (blank(id) || blank(title) || blank(description) || sourceTask == null || capturedAt == null || freshness == null || schemaVersion < 1) throw new IllegalArgumentException("Evidence identity, source, description, freshness and schema are required");
        classification = Objects.requireNonNull(classification, "classification"); confidence = Objects.requireNonNull(confidence, "confidence");
        staticFingerprint = staticFingerprint == null ? Optional.empty() : staticFingerprint; runtimeFingerprint = runtimeFingerprint == null ? Optional.empty() : runtimeFingerprint;
        fields = boundedMap(fields, MAX_FIELDS); limitations = boundedList(limitations, MAX_LIMITATIONS); relatedIds = boundedList(relatedIds, MAX_RELATIONS); attribution = attribution == null ? Optional.empty() : attribution;
        if (classification == Classification.HYPOTHESIS && limitations.isEmpty()) throw new IllegalArgumentException("Hypothesis evidence requires limitations or verification requirements");
        if (classification == Classification.EXACT_ATTRIBUTION && attribution.isEmpty()) throw new IllegalArgumentException("Exact attribution requires attribution");
    }
    static boolean blank(String value) { return value == null || value.isBlank(); }
    static Map<String,String> boundedMap(Map<String,String> value, int max) { if (value == null) return Map.of(); if (value.size() > max) throw new IllegalArgumentException("Too many fields"); TreeMap<String,String> result=new TreeMap<>(); value.forEach((k,v)-> { if (blank(k)) throw new IllegalArgumentException("Blank field"); result.put(k, v == null ? "" : v); }); return Map.copyOf(result); }
    static List<String> boundedList(List<String> value, int max) { if (value == null) return List.of(); if (value.size()>max) throw new IllegalArgumentException("Too many list values"); return List.copyOf(value.stream().filter(Objects::nonNull).toList()); }
}

package com.soumyajit.gradlemc.investigation;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

/** Safe opaque local-session identifier. */
public record InvestigationId(String value) implements Comparable<InvestigationId> {
    public static final int MAX_LENGTH = 80;
    public InvestigationId { validate(value, "investigation ID"); }
    public static InvestigationId generate(Clock clock, UUID uuid) {
        if (clock == null || uuid == null) throw new IllegalArgumentException("clock and UUID are required");
        return new InvestigationId("inv-" + clock.instant().toEpochMilli() + "-" + uuid.toString().replace("-", ""));
    }
    public static InvestigationId parse(String value) { return new InvestigationId(value); }
    static void validate(String value, String label) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH || !value.matches("[a-z][a-z0-9-]{0,79}")) throw new IllegalArgumentException("Invalid " + label);
        String lower = value.toLowerCase(Locale.ROOT);
        if (value.contains("..") || value.endsWith(".") || value.endsWith(" ") || lower.matches("(con|prn|aux|nul|com[1-9]|lpt[1-9])")) throw new IllegalArgumentException("Unsafe " + label);
    }
    @Override public int compareTo(InvestigationId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}

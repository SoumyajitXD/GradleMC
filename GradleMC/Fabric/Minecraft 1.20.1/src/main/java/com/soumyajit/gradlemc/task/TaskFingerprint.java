package com.soumyajit.gradlemc.task;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;

/** Deterministic schema-v1 fingerprint for normalized, non-sensitive input values. */
public final class TaskFingerprint {
    private TaskFingerprint() { }
    public static String sha256(Collection<String> values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            values.stream().map(value -> value == null ? "" : value).sorted().forEach(value -> { digest.update(value.getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0); });
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
}

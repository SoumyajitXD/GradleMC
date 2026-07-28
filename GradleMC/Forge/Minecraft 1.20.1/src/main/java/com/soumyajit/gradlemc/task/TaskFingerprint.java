package com.soumyajit.gradlemc.task;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Length-prefixed canonical SHA-256 input encoding; never silently omits a declared value. */
public final class TaskFingerprint {
    private TaskFingerprint() { }

    public static String sha256(Map<String, String> fields) {
        Objects.requireNonNull(fields, "fields");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, String> entry : new TreeMap<>(fields).entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("Fingerprint field is incomplete");
                }
                update(digest, entry.getKey());
                update(digest, entry.getValue());
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24)); digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8)); digest.update((byte) bytes.length); digest.update(bytes);
    }
}

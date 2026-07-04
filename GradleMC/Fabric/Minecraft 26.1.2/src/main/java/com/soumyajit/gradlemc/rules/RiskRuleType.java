package com.soumyajit.gradlemc.rules;

import java.util.Locale;
import java.util.Optional;

public enum RiskRuleType {
    MOD_PRESENT,
    MOD_MISSING,
    MOD_GROUP_COUNT,
    CLIENT_ONLY_ON_SERVER,
    SERVER_ONLY_ON_CLIENT,
    CONFIG_FILE_EXISTS;

    public static Optional<RiskRuleType> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(RiskRuleType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}

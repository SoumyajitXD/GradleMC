package com.soumyajit.gradlemc.rules;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.ManagedPathSafety;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RiskRuleLoader {
    private static final long MAX_RULE_FILE_BYTES = 1024L * 1024L;
    private static final int MAX_RULES = 256;
    private static final int MAX_FIELD_LENGTH = 1024;
    private static final int MAX_MOD_IDS_PER_RULE = 64;
    private static final Gson GSON = new Gson();
    private static RiskRuleSet cachedRules;

    private RiskRuleLoader() {
    }

    public static synchronized RiskRuleSet current() {
        if (cachedRules == null) {
            cachedRules = load();
        }
        return cachedRules;
    }

    public static synchronized RiskRuleSet reload() {
        cachedRules = load();
        return cachedRules;
    }

    public static Path rulesPath() {
        String configured = sanitizeFileName(GradleMCConfig.RULES_FILE_NAME.get());
        return GradleMcPaths.rulesDirectory().resolve(configured).normalize();
    }

    public static Path examplePath() {
        return GradleMcPaths.rulesDirectory().resolve("gradlemc-rules.example.json").normalize();
    }

    public static boolean writeExampleIfMissing() throws IOException {
        Path path = examplePath();
        ManagedPathSafety.ensureDirectory(GradleMcPaths.gameDirectory(), GradleMcPaths.rulesDirectory());
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(exampleJson(), writer);
        }
        return true;
    }

    private static RiskRuleSet load() {
        Path path = rulesPath();
        List<CheckResult> loadResults = new ArrayList<>();
        if (!Files.exists(path)) {
            loadResults.add(CheckResult.of(
                    Severity.INFO,
                    CheckCategory.CONFIG,
                    "Risk rule file not found",
                    path.toString(),
                    "Create this file or run /gradlemc rules example to generate a template."
            ));
            return new RiskRuleSet(path, List.of(), loadResults, Instant.now());
        }
        try { ManagedPathSafety.ensureDirectory(GradleMcPaths.gameDirectory(), GradleMcPaths.rulesDirectory()); }
        catch (IOException exception) { return unavailable(path, loadResults, "Risk rule directory is unavailable"); }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path) || !Files.isReadable(path)) {
            loadResults.add(CheckResult.of(
                    Severity.WARN,
                    CheckCategory.CONFIG,
                    "Risk rule file is not readable",
                    path.toString(),
                    "Check file permissions and keep the rule file inside gradlemc/rules."
            ));
            return new RiskRuleSet(path, List.of(), loadResults, Instant.now());
        }
        try { if (Files.size(path) > MAX_RULE_FILE_BYTES) return unavailable(path, loadResults, "Risk rule file exceeds the 1 MiB safe limit"); }
        catch (IOException exception) { return unavailable(path, loadResults, "Risk rule file size could not be read"); }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            JsonObject object = root.isJsonObject() ? root.getAsJsonObject() : null;
            JsonArray rules = object == null || !object.has("rules") || !object.get("rules").isJsonArray()
                    ? null
                    : object.getAsJsonArray("rules");
            if (rules == null) {
                loadResults.add(CheckResult.of(
                        Severity.WARN,
                        CheckCategory.CONFIG,
                        "Risk rule file has no rules array",
                        path.toString(),
                        "Use {\"rules\": [...]} as the top-level shape."
                ));
                return new RiskRuleSet(path, List.of(), loadResults, Instant.now());
            }

            List<RiskRule> parsed = new ArrayList<>();
            int index = 0;
            for (JsonElement element : rules) {
                index++;
                if (index > MAX_RULES) { invalid(index, "Rule limit of " + MAX_RULES + " reached", loadResults); break; }
                parseRule(element, index, parsed, loadResults);
            }
            loadResults.add(CheckResult.of(
                    Severity.INFO,
                    CheckCategory.CONFIG,
                    "Risk rules loaded",
                    parsed.size() + " valid rule(s) from " + path,
                    "Run /gradlemc rules reload after editing the file."
            ));
            return new RiskRuleSet(path, List.copyOf(parsed), List.copyOf(loadResults), Instant.now());
        } catch (JsonSyntaxException | IllegalStateException | NumberFormatException | UnsupportedOperationException exception) {
            loadResults.add(CheckResult.of(
                    Severity.WARN,
                    CheckCategory.CONFIG,
                    "Risk rule file could not be parsed",
                    safeMessage(exception),
                    "Fix the JSON and run /gradlemc rules reload. Bad rules do not stop the game."
            ));
            return new RiskRuleSet(path, List.of(), loadResults, Instant.now());
        } catch (IOException exception) {
            loadResults.add(CheckResult.of(
                    Severity.WARN,
                    CheckCategory.CONFIG,
                    "Risk rule file could not be read",
                    safeMessage(exception),
                    "Check file permissions and run /gradlemc rules reload."
            ));
            return new RiskRuleSet(path, List.of(), loadResults, Instant.now());
        }
    }

    private static RiskRuleSet unavailable(Path path, List<CheckResult> results, String detail) {
        results.add(CheckResult.of(Severity.WARN, CheckCategory.CONFIG, "Risk rule file is unavailable", detail,
                "Keep the rule file inside gradlemc/rules as a regular file."));
        return new RiskRuleSet(path, List.of(), results, Instant.now());
    }

    private static void parseRule(JsonElement element, int index, List<RiskRule> rules, List<CheckResult> loadResults) {
        if (!element.isJsonObject()) {
            invalid(index, "Rule is not a JSON object", loadResults);
            return;
        }
        JsonObject object = element.getAsJsonObject();
        String id = stringValue(object, "id", "rule-" + index);
        RiskRuleType type = RiskRuleType.parse(stringValue(object, "type", "")).orElse(null);
        if (type == null) {
            invalid(index, "Unknown or missing type for " + id, loadResults);
            return;
        }
        Severity severity = parseSeverity(stringValue(object, "severity", "WARN"));
        String message = stringValue(object, "message", id);
        String suggestion = stringValue(object, "suggestion", "Review this rule before release.");
        String modId = stringValue(object, "modId", "").toLowerCase(Locale.ROOT);
        List<String> modIds = stringArray(object, "modIds");
        int maxPresent = intValue(object, "maxPresent", 0);
        String configFile = stringValue(object, "configFile", stringValue(object, "file", ""));
        String versionRange = stringValue(object, "versionRange", "");
        String dependencyModId = stringValue(object, "dependencyModId", stringValue(object, "dependency", "")).toLowerCase(Locale.ROOT);
        boolean expectExists = booleanValue(object, "expectExists", true);

        if (requiresModId(type) && modId.isBlank()) {
            invalid(index, "Missing modId for " + id, loadResults);
            return;
        }
        if (type == RiskRuleType.MOD_GROUP_COUNT && (modIds.isEmpty() || maxPresent < 0)) {
            invalid(index, "mod_group_count requires modIds and maxPresent for " + id, loadResults);
            return;
        }
        if (type == RiskRuleType.CONFIG_FILE_EXISTS && configFile.isBlank()) {
            invalid(index, "config_file_exists requires configFile for " + id, loadResults);
            return;
        }
        if ((type == RiskRuleType.ALL_MODS_PRESENT || type == RiskRuleType.ANY_MOD_PRESENT) && modIds.isEmpty()) {
            invalid(index, type.name().toLowerCase(Locale.ROOT) + " requires modIds for " + id, loadResults);
            return;
        }
        if ((type == RiskRuleType.MOD_VERSION_IN_RANGE || type == RiskRuleType.MOD_VERSION_OUTSIDE_RANGE) && (modId.isBlank() || versionRange.isBlank())) {
            invalid(index, type.name().toLowerCase(Locale.ROOT) + " requires modId and versionRange for " + id, loadResults);
            return;
        }
        if ((type == RiskRuleType.DEPENDENCY_PRESENT || type == RiskRuleType.DEPENDENCY_ABSENT) && (modId.isBlank() || dependencyModId.isBlank())) {
            invalid(index, type.name().toLowerCase(Locale.ROOT) + " requires modId and dependencyModId for " + id, loadResults);
            return;
        }

        rules.add(new RiskRule(id, type, modId, modIds, maxPresent, configFile, versionRange, dependencyModId, expectExists, severity, message, suggestion));
    }

    private static boolean requiresModId(RiskRuleType type) {
        return type == RiskRuleType.MOD_PRESENT
                || type == RiskRuleType.MOD_MISSING
                || type == RiskRuleType.CLIENT_ONLY_ON_SERVER
                || type == RiskRuleType.SERVER_ONLY_ON_CLIENT
                || type == RiskRuleType.MOD_VERSION_IN_RANGE
                || type == RiskRuleType.MOD_VERSION_OUTSIDE_RANGE
                || type == RiskRuleType.DEPENDENCY_PRESENT
                || type == RiskRuleType.DEPENDENCY_ABSENT;
    }

    private static void invalid(int index, String detail, List<CheckResult> loadResults) {
        loadResults.add(CheckResult.of(
                Severity.WARN,
                CheckCategory.CONFIG,
                "Invalid risk rule #" + index,
                detail,
                "Fix or remove this rule and run /gradlemc rules reload."
        ));
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return fallback;
        String text = value.getAsString();
        return text.length() <= MAX_FIELD_LENGTH ? text : text.substring(0, MAX_FIELD_LENGTH);
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsInt();
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
    }

    private static List<String> stringArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            if (values.size() >= MAX_MOD_IDS_PER_RULE) break;
            if (!element.isJsonNull()) {
                String text = element.getAsString().trim().toLowerCase(Locale.ROOT);
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
        }
        return values;
    }

    private static Severity parseSeverity(String value) {
        try {
            return Severity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Severity.WARN;
        }
    }

    private static String sanitizeFileName(String value) {
        if (value == null || value.isBlank() || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}") || value.contains("..")) {
            return "gradlemc-rules.json";
        }
        return value;
    }

    private static JsonObject exampleJson() {
        JsonObject root = new JsonObject();
        JsonArray rules = new JsonArray();
        JsonObject oculus = new JsonObject();
        oculus.addProperty("id", "oculus-client-only");
        oculus.addProperty("type", "client_only_on_server");
        oculus.addProperty("modId", "oculus");
        oculus.addProperty("severity", "FAIL");
        oculus.addProperty("message", "Oculus is client-side and should not be in a dedicated server pack.");
        oculus.addProperty("suggestion", "Remove Oculus from the server mods folder.");
        rules.add(oculus);

        JsonObject group = new JsonObject();
        group.addProperty("id", "too-many-horror-ai-mods");
        group.addProperty("type", "mod_group_count");
        JsonArray modIds = new JsonArray();
        modIds.add("goatman");
        modIds.add("cave_dweller");
        modIds.add("from_the_fog");
        modIds.add("man");
        modIds.add("siren_head");
        group.add("modIds", modIds);
        group.addProperty("maxPresent", 3);
        group.addProperty("severity", "WARN");
        group.addProperty("message", "Multiple stalker/horror AI mods are present.");
        group.addProperty("suggestion", "Benchmark TPS and entity counts before release.");
        rules.add(group);
        root.add("rules", rules);
        return root;
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}

package com.soumyajit.gradlemc.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Exercises migration and atomic persistence against a temporary Fabric-config-shaped properties file. */
public final class GradleMCConfigSelfTest {
    private GradleMCConfigSelfTest() {
    }

    public static void run() {
        try {
            Path root = Files.createTempDirectory("gradlemc-config-test-");
            Path config = root.resolve("gradlemc.properties");
            GradleMCConfig.ConfigSpec spec = new GradleMCConfig.ConfigSpec(config);
            spec.load();
            require(Files.isRegularFile(config), "missing config must produce defaults");
            require(!GradleMCConfig.OVERLAY_SHOW_TITLE.get(), "branding default must be disabled");
            require(!GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get(), "average FPS default must be independent and disabled");

            Files.writeString(config, "reportsEnabled=false\nmaxPerfSeconds=not-a-number\ncustom.future.field=retain\noverlay.showFps=false\n",
                    StandardCharsets.ISO_8859_1);
            spec.load();
            require(!GradleMCConfig.REPORTS_ENABLED.get(), "valid legacy setting must survive migration");
            require(GradleMCConfig.MAX_PERF_SECONDS.get() == 600, "one malformed field must use its default only");
            require(!GradleMCConfig.OVERLAY_SHOW_FPS.get(), "legacy overlay choice must survive migration");
            require(!GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get(), "missing new average setting must use safe default");
            spec.save();
            Properties saved = new Properties();
            try (var input = Files.newInputStream(config)) {
                saved.load(input);
            }
            require("retain".equals(saved.getProperty("custom.future.field")), "unknown future field must be retained");
            require("2".equals(saved.getProperty("schemaVersion")), "migration must be persisted once");

            spec.load();
            require(!GradleMCConfig.REPORTS_ENABLED.get(), "repeated migration must be idempotent");
            Files.writeString(config, "schemaVersion=2\noverlay.showFps=\\u00ZZ\n", StandardCharsets.UTF_8);
            spec.load();
            require(Files.isRegularFile(config.resolveSibling("gradlemc.properties.corrupt")), "malformed complete config must be preserved for recovery");
            require(Files.isRegularFile(config), "malformed complete config must be replaced with a fresh valid default file");
            Properties recovered = new Properties();
            try (var input = Files.newInputStream(config)) { recovered.load(input); }
            require("2".equals(recovered.getProperty("schemaVersion")), "recovered config must be independently parseable");

            for (int index = 0; index < 100; index++) {
                GradleMCConfig.REPORTS_ENABLED.set((index & 1) == 0);
                spec.save();
            }
            spec.load();
            require(!GradleMCConfig.REPORTS_ENABLED.get(), "rapid synchronous changes must reload the final value without a stale delayed save");
            Files.deleteIfExists(config);
            Files.deleteIfExists(config.resolveSibling("gradlemc.properties.corrupt"));
            Files.deleteIfExists(root);
        } catch (IOException exception) {
            throw new AssertionError("configuration persistence test failed", exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

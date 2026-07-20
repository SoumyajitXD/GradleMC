package com.soumyajit.gradlemc.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class GradleMCConfigMigrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void legacyConfigIsPersistedOnceWithSafeNewOverlayDefaultsAndUnknownFields() throws Exception {
        Path config = temporaryDirectory.resolve("gradlemc.properties");
        Files.writeString(config, "reportsEnabled=false\noverlay.showFps=true\ncustom.future.field=retain\n", StandardCharsets.UTF_8);

        GradleMCConfig.ConfigSpec first = new GradleMCConfig.ConfigSpec(config);
        first.load();
        assertFalse(GradleMCConfig.REPORTS_ENABLED.get());
        assertTrue(GradleMCConfig.OVERLAY_SHOW_FPS.get());
        assertFalse(GradleMCConfig.OVERLAY_SHOW_TITLE.get());
        assertFalse(GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get());

        String onceMigrated = Files.readString(config, StandardCharsets.UTF_8);
        Properties stored = new Properties();
        try (var input = Files.newInputStream(config)) {
            stored.load(input);
        }
        assertEquals("2", stored.getProperty("schemaVersion"));
        assertEquals("retain", stored.getProperty("custom.future.field"));

        GradleMCConfig.ConfigSpec second = new GradleMCConfig.ConfigSpec(config);
        second.load();
        assertEquals(onceMigrated, Files.readString(config, StandardCharsets.UTF_8));
    }
}

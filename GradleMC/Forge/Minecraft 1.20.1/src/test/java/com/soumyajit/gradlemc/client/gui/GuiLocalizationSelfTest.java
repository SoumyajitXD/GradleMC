package com.soumyajit.gradlemc.client.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Guards the Forge GUI against raw keys and accidental loader-specific labels. */
public final class GuiLocalizationSelfTest {
    private GuiLocalizationSelfTest() {
    }

    public static void run() throws IOException {
        Path source = Path.of("src/main/java/com/soumyajit/gradlemc/client/gui/LegacyGradleMCScreen.java");
        Path language = Path.of("src/main/resources/assets/gradlemc/lang/en_us.json");
        String java = Files.readString(source);
        String json = Files.readString(language);
        check(!java.contains("screen.gradlemc.label.fabric"), "Forge screen must not use Fabric label");
        check(!json.contains("screen.gradlemc.label.fabric"), "Forge language must not expose Fabric label");
        Matcher keys = Pattern.compile("screen\\.gradlemc\\.[a-z0-9_.]+") .matcher(java);
        while (keys.find()) {
            String key = keys.group();
            if (key.endsWith(".")) continue; // dynamic enum suffixes are validated by their enum-specific tests.
            check(json.contains("\"" + key + "\""), "missing GUI translation: " + key);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

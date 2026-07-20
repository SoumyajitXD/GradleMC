package com.soumyajit.gradlemc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FabricMetadataTest {
    @Test
    void processedFabricMetadataHasTheExactReleaseIdentity() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("fabric.mod.json")) {
            assertNotNull(input, "fabric.mod.json must be on the production resource path");
            JsonObject metadata = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("gradlemc", metadata.get("id").getAsString());
            assertEquals("1.0.1", metadata.get("version").getAsString());
            assertEquals("GradleMC", metadata.get("name").getAsString());
            assertEquals("Apache-2.0", metadata.get("license").getAsString());
            assertEquals("GradleMC_logo.png", metadata.get("icon").getAsString());
            assertEquals("*", metadata.get("environment").getAsString());
            assertTrue(metadata.getAsJsonObject("entrypoints").getAsJsonArray("main").contains(new com.google.gson.JsonPrimitive("com.soumyajit.gradlemc.fabric.GradleMCFabric")));
            assertTrue(metadata.getAsJsonObject("entrypoints").getAsJsonArray("client").contains(new com.google.gson.JsonPrimitive("com.soumyajit.gradlemc.fabric.GradleMCFabricClient")));
            JsonObject dependencies = metadata.getAsJsonObject("depends");
            assertEquals(">=0.19.3", dependencies.get("fabricloader").getAsString());
            assertEquals(">=1.20.1 <1.20.2", dependencies.get("minecraft").getAsString());
            assertEquals(">=17", dependencies.get("java").getAsString());
            assertEquals(">=0.92.9+1.20.1", dependencies.get("fabric-api").getAsString());
            assertFalse(metadata.toString().contains("${"), "processed metadata must not contain expansion placeholders");
            assertFalse(metadata.toString().toLowerCase(java.util.Locale.ROOT).contains("forge"));
            assertNotNull(getClass().getClassLoader().getResource("GradleMC_logo.png"));
        }
    }

    @Test
    void englishLanguageResourceIsValidAndContainsDynamicKeys() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("assets/gradlemc/lang/en_us.json")) {
            assertNotNull(input);
            JsonObject language = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("Average FPS", language.get("screen.gradlemc.label.rolling_fps").getAsString());
            for (String level : java.util.List.of("low", "medium", "high", "extreme")) {
                assertTrue(language.has("screen.gradlemc.threat." + level));
            }
            assertTrue(language.has("key.categories.gradlemc"));
        }
    }
}

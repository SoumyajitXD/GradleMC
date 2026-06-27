package com.soumyajit.gradlemc.util;

import com.soumyajit.gradlemc.config.GradleMCConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class GradleMcPaths {
    private GradleMcPaths() {
    }

    public static Path configDirectory() {
        return FMLPaths.CONFIGDIR.get().normalize();
    }

    public static Path gradleMcDirectory() {
        return configDirectory().resolve("gradlemc").normalize();
    }

    public static Path reportDirectory() {
        Path configDir = configDirectory();
        Path configured = configDir.resolve(safeReportDirectoryName()).normalize();
        if (!configured.startsWith(configDir)) {
            return gradleMcDirectory().resolve("reports").normalize();
        }
        return configured;
    }

    private static String safeReportDirectoryName() {
        String name = GradleMCConfig.REPORT_DIRECTORY_NAME.get();
        if (name == null || name.isBlank() || name.contains("..")) {
            return "gradlemc/reports";
        }
        return name.replace('\\', '/');
    }
}

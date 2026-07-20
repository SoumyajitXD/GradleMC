package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.SharedConstants;

import java.util.List;

final class ReportEnvironment {
    private ReportEnvironment() {
    }

    static List<String> lines() {
        return List.of(
                "Product: " + GradleMC.PRODUCT_NAME,
                "Version: " + GradleMC.version(),
                "Variant: " + GradleMC.CURRENT_DISPLAY_VARIANT,
                "Minecraft: " + SharedConstants.getCurrentVersion().getName(),
                "Loader: " + GradleMC.CURRENT_LOADER_NAME,
                "Fabric Loader: " + GradleMC.fabricLoaderVersion(),
                "Java: " + System.getProperty("java.version", "unknown"),
                "Physical side: " + net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType(),
                "Output root: " + GradleMcPaths.displayPath(GradleMcPaths.gradleMcDirectory()),
                "Loaded mods: " + net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().size()
        );
    }
}

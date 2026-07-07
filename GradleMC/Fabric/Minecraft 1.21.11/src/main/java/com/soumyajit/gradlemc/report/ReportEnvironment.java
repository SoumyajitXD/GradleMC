package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.util.List;

final class ReportEnvironment {
    private ReportEnvironment() {
    }

    static List<String> lines() {
        return List.of(
                "Product: " + GradleMC.PRODUCT_NAME,
                "Version: " + modVersion(),
                "Variant: " + GradleMC.currentDisplayVariant(),
                "Minecraft: " + SharedConstants.getCurrentVersion().name(),
                "Loader: " + GradleMC.CURRENT_LOADER_NAME,
                "Fabric Loader: " + loaderVersion(),
                "Java: " + System.getProperty("java.version", "unknown"),
                "Physical side: " + FabricLoader.getInstance().getEnvironmentType(),
                "Output root: " + GradleMcPaths.gradleMcDirectory(),
                "Loaded mods: " + FabricLoader.getInstance().getAllMods().size()
        );
    }

    private static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(GradleMC.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String loaderVersion() {
        return FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}

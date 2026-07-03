package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.minecraft.MinecraftQuiltLoader;
import net.minecraft.SharedConstants;

import java.util.List;

final class ReportEnvironment {
    private ReportEnvironment() {
    }

    static List<String> lines() {
        return List.of(
                "Product: " + GradleMC.PRODUCT_NAME,
                "Version: " + modVersion(),
                "Variant: " + GradleMC.CURRENT_DISPLAY_VARIANT,
                "Minecraft: " + SharedConstants.getCurrentVersion().getName(),
                "Loader: " + GradleMC.CURRENT_LOADER_NAME,
                "Quilt Loader: " + loaderVersion(),
                "Java: " + System.getProperty("java.version", "unknown"),
                "Physical side: " + MinecraftQuiltLoader.getEnvironmentType(),
                "Output root: " + GradleMcPaths.gradleMcDirectory(),
                "Loaded mods: " + QuiltLoader.getAllMods().size()
        );
    }

    private static String modVersion() {
        return QuiltLoader
                .getModContainer(GradleMC.MOD_ID)
                .map(container -> container.metadata().version().raw())
                .orElse("unknown");
    }

    private static String loaderVersion() {
        return QuiltLoader
                .getModContainer("quilt_loader")
                .map(container -> container.metadata().version().raw())
                .orElse("unknown");
    }
}

package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.versions.forge.ForgeVersion;

import java.util.List;

final class ReportEnvironment {
    private ReportEnvironment() {
    }

    static List<String> lines() {
        return List.of(
                "Product: " + GradleMC.PRODUCT_NAME,
                "Version: " + modVersion(),
                "Variant: " + GradleMC.CURRENT_DISPLAY_VARIANT,
                "Minecraft: " + GradleMC.CURRENT_MINECRAFT_VERSION,
                "Loader: " + GradleMC.CURRENT_LOADER_NAME,
                "Forge: " + ForgeVersion.getVersion(),
                "Java: " + System.getProperty("java.version", "unknown"),
                "Physical side: " + FMLEnvironment.dist,
                "Output root: " + GradleMcPaths.gradleMcDirectory(),
                "Loaded mods: " + ModList.get().getMods().size()
        );
    }

    private static String modVersion() {
        return ModList.get().getModContainerById(GradleMC.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}

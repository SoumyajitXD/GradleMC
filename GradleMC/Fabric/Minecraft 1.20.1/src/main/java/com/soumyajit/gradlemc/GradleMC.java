package com.soumyajit.gradlemc;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

public final class GradleMC {
    public static final String PRODUCT_NAME = "GradleMC";
    public static final String MOD_ID = "gradlemc";
    public static final String CURRENT_LOADER_NAME = "Fabric";
    public static final String CURRENT_MINECRAFT_VERSION = "1.20.1";
    public static final String CURRENT_DISPLAY_VARIANT = "Fabric 1.20.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    private GradleMC() {
    }

    /** Runtime identity comes from processed loader metadata, the same source shown to users by Fabric Loader. */
    public static String version() {
        return modVersion(MOD_ID);
    }

    public static String fabricLoaderVersion() {
        return modVersion("fabricloader");
    }

    private static String modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}

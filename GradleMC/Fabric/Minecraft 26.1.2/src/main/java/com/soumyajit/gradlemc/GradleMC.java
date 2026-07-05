package com.soumyajit.gradlemc;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import org.slf4j.Logger;

public final class GradleMC {
    public static final String PRODUCT_NAME = "GradleMC";
    public static final String MOD_ID = "gradlemc";
    public static final String CURRENT_LOADER_NAME = "Fabric";
    public static final Logger LOGGER = LogUtils.getLogger();

    private GradleMC() {
    }

    public static String currentMinecraftVersion() {
        return SharedConstants.getCurrentVersion().name();
    }

    public static String currentVariantId() {
        return CURRENT_LOADER_NAME.toLowerCase(java.util.Locale.ROOT) + "-" + currentMinecraftVersion();
    }

    public static String currentDisplayVariant() {
        return CURRENT_LOADER_NAME + " " + currentMinecraftVersion();
    }
}

package com.soumyajit.gradlemc;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class GradleMC {
    public static final String PRODUCT_NAME = "GradleMC";
    public static final String MOD_ID = "gradlemc";
    public static final String CURRENT_LOADER_NAME = "Fabric";
    public static final String CURRENT_MINECRAFT_VERSION = "1.20.1";
    public static final String CURRENT_VARIANT_ID = "fabric-1.20.1";
    public static final String CURRENT_DISPLAY_VARIANT = "Fabric 1.20.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    private GradleMC() {
    }
}

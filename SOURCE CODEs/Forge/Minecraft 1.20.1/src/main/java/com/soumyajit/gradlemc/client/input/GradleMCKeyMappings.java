package com.soumyajit.gradlemc.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class GradleMCKeyMappings {
    public static final String CATEGORY = "key.categories.gradlemc";
    public static final KeyMapping OPEN_GUI = new KeyMapping(
            "key.gradlemc.open_gui",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    private GradleMCKeyMappings() {
    }
}

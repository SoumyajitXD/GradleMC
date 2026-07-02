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
    public static final KeyMapping TOGGLE_OVERLAY = new KeyMapping(
            "key.gradlemc.toggle_overlay",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY
    );
    public static final KeyMapping CYCLE_OVERLAY_POSITION = new KeyMapping(
            "key.gradlemc.cycle_overlay_position",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY
    );
    public static final KeyMapping TOGGLE_OVERLAY_MODE = new KeyMapping(
            "key.gradlemc.toggle_overlay_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY
    );
    public static final KeyMapping START_STOP_QUICK_FPS_SAMPLE = new KeyMapping(
            "key.gradlemc.quick_fps_sample",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY
    );

    private GradleMCKeyMappings() {
    }
}

package com.soumyajit.gradlemc.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class GradleMCKeyMappings {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("gradlemc", "gradlemc"));
    public static final KeyMapping OPEN_GUI = new KeyMapping(
            "key.gradlemc.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );
    public static final KeyMapping TOGGLE_OVERLAY = new KeyMapping(
            "key.gradlemc.toggle_overlay",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping CYCLE_OVERLAY_POSITION = new KeyMapping(
            "key.gradlemc.cycle_overlay_position",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping TOGGLE_OVERLAY_MODE = new KeyMapping(
            "key.gradlemc.toggle_overlay_mode",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping START_STOP_QUICK_FPS_SAMPLE = new KeyMapping(
            "key.gradlemc.quick_fps_sample",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private GradleMCKeyMappings() {
    }
}

package com.soumyajit.gradlemc.client.input

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraftforge.client.settings.KeyConflictContext
import org.lwjgl.glfw.GLFW

/** Client-only mappings. Forge persists user customisations through the normal key mapping system. */
object GradleMCKeyMappings {
    const val CATEGORY = "key.categories.gradlemc"

    val openGui = KeyMapping(
        "key.gradlemc.open_gui",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        CATEGORY,
    )

    val toggleOverlay = KeyMapping(
        "key.gradlemc.toggle_overlay",
        KeyConflictContext.IN_GAME,
        InputConstants.UNKNOWN,
        CATEGORY,
    )
}

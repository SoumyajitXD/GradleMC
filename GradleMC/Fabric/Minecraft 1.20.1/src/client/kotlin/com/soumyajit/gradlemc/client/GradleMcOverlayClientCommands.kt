package com.soumyajit.gradlemc.client

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.soumyajit.gradlemc.config.GradleMcConfig
import com.soumyajit.gradlemc.config.GradleMcConfigSnapshot
import com.soumyajit.gradlemc.performance.PerformanceService
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.network.chat.Component

/** Client-only overlay route.  Its distinct root avoids shadowing server /gradlemc commands. */
internal object GradleMcOverlayClientCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("gradlemc-overlay")
                .executes { status(it.source) }
                .then(literal("status").executes { status(it.source) })
                .then(change("on") { it.copy(overlayEnabled = true) })
                .then(change("off") { it.copy(overlayEnabled = false) })
                .then(change("toggle") { it.copy(overlayEnabled = !it.overlayEnabled) })
                .then(component("title") { c, value -> c.copy(overlayShowTitle = value) })
                .then(component("fps") { c, value -> c.copy(overlayShowFps = value) })
                .then(component("average") { c, value -> c.copy(overlayShowAverageFps = value) })
                .then(literal("window").then(argument("seconds", IntegerArgumentType.integer()).executes { update(it.source) { c -> c.copy(overlaySamplingWindowSeconds = IntegerArgumentType.getInteger(it, "seconds")) } }))
                .then(change("reset") { GradleMcConfigSnapshot() }))
        }
    }

    private fun change(word: String, transform: (GradleMcConfigSnapshot) -> GradleMcConfigSnapshot) = literal(word).executes { update(it.source, transform) }
    private fun component(word: String, transform: (GradleMcConfigSnapshot, Boolean) -> GradleMcConfigSnapshot) = literal(word).then(literal("on").executes { update(it.source) { c -> transform(c, true) } }).then(literal("off").executes { update(it.source) { c -> transform(c, false) } })
    private fun update(source: net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, transform: (GradleMcConfigSnapshot) -> GradleMcConfigSnapshot): Int = try { GradleMcConfig.update(transform); PerformanceService.configureFromConfig(); status(source) } catch (e: Exception) { source.sendError(Component.literal("Unable to persist overlay settings: ${e.message ?: e.javaClass.simpleName}")); 0 }
    private fun status(source: net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource): Int { val c = GradleMcConfig.current(); source.sendFeedback(Component.literal("GradleMC overlay ${if (c.overlayEnabled) "on" else "off"}; title=${c.overlayShowTitle}, fps=${c.overlayShowFps}, average=${c.overlayShowAverageFps}, window=${c.overlaySamplingWindowSeconds}s.")); return 1 }
}

package com.soumyajit.gradlemc.network

import com.soumyajit.gradlemc.GradleMC
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/** Common side owns only the S2C send boundary; the receiver is client-source-set only. */
object GradleMcNetwork {
    val OPEN_GUI: ResourceLocation = ResourceLocation(GradleMC.MOD_ID, "open_gui")
    /** Client-only render sampling is requested through this S2C boundary. */
    val FPS_TEST_ACTION: ResourceLocation = ResourceLocation(GradleMC.MOD_ID, "fps_test_action")
    fun registerCommon() = Unit

    /** A positive duration starts a test; null requests that the client stops one. */
    fun requestFpsTest(player: ServerPlayer, seconds: Int?): Boolean {
        if (!ServerPlayNetworking.canSend(player, FPS_TEST_ACTION)) return false
        val payload = PacketByteBufs.create()
        payload.writeVarInt(seconds ?: 0)
        ServerPlayNetworking.send(player, FPS_TEST_ACTION, payload)
        return true
    }

    fun openGui(player: ServerPlayer): Boolean {
        if (!ServerPlayNetworking.canSend(player, OPEN_GUI)) return false
        ServerPlayNetworking.send(player, OPEN_GUI, PacketByteBufs.create())
        return true
    }
}

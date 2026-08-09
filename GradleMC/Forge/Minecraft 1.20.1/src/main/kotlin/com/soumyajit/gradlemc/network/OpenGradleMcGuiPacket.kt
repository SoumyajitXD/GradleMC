package com.soumyajit.gradlemc.network

import com.soumyajit.gradlemc.GradleMC
import com.soumyajit.gradlemc.client.GuiBridgeResult
import com.soumyajit.gradlemc.client.GuiOpenOrigin
import com.soumyajit.gradlemc.client.GradleMcGuiBridge
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

/** Empty server-to-client payload requesting the receiving client open its local diagnostics screen. */
class OpenGradleMcGuiPacket private constructor() {
    companion object {
        val INSTANCE = OpenGradleMcGuiPacket()

        @JvmStatic
        fun encode(packet: OpenGradleMcGuiPacket, buffer: FriendlyByteBuf) = Unit

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): OpenGradleMcGuiPacket = INSTANCE

        @JvmStatic
        fun handle(packet: OpenGradleMcGuiPacket, contextSupplier: Supplier<NetworkEvent.Context>) {
            val context = contextSupplier.get()
            GradleMC.LOGGER.debug("/gradlemc gui packet received (direction={})", context.direction)
            if (isEligibleDirection(context.direction)) {
                GradleMC.LOGGER.debug("GradleMC GUI packet client work enqueued")
                context.enqueueWork {
                    GradleMC.LOGGER.debug("Running /gradlemc gui packet work on client thread")
                    if (GradleMcGuiBridge.requestOpen(GuiOpenOrigin.SERVER_COMMAND) == GuiBridgeResult.NO_OPENER) {
                        GradleMC.LOGGER.warn("/gradlemc gui packet skipped: no client opener is registered")
                    }
                }
            } else {
                GradleMC.LOGGER.warn("Rejected GradleMC GUI packet on direction {}", context.direction)
            }
            context.packetHandled = true
        }

        @JvmStatic
        fun isEligibleDirection(direction: NetworkDirection): Boolean = direction == NetworkDirection.PLAY_TO_CLIENT
    }
}

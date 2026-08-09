package com.soumyajit.gradlemc.network

import com.soumyajit.gradlemc.GradleMC
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel

/** Common network entry point for requests that must cross from the logical server to a client. */
object GradleMcNetwork {
    private const val PROTOCOL_VERSION = "1"

    private val channel: SimpleChannel = NetworkRegistry.ChannelBuilder
        .named(ResourceLocation(GradleMC.MOD_ID, "main"))
        .networkProtocolVersion { PROTOCOL_VERSION }
        .clientAcceptedVersions { version -> version == PROTOCOL_VERSION }
        .serverAcceptedVersions { version -> version == PROTOCOL_VERSION }
        .simpleChannel()

    @Volatile
    private var registered = false

    @Synchronized
    fun register() {
        if (registered) {
            GradleMC.LOGGER.warn("Prevented duplicate GradleMC packet registration")
            return
        }

        channel.messageBuilder(OpenGradleMcGuiPacket::class.java, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(OpenGradleMcGuiPacket::encode)
            .decoder(OpenGradleMcGuiPacket::decode)
            .consumerNetworkThread(OpenGradleMcGuiPacket::handle)
            .add()
        registered = true
        GradleMC.LOGGER.info("GradleMC GUI packet registered (id=0, direction=PLAY_TO_CLIENT)")
    }

    fun openGui(player: ServerPlayer) {
        check(registered) { "GradleMC network was not registered before an open-GUI request." }
        GradleMC.LOGGER.debug("Sending GradleMC GUI packet to {}", player.scoreboardName)
        channel.send(PacketDistributor.PLAYER.with { player }, OpenGradleMcGuiPacket.INSTANCE)
    }
}

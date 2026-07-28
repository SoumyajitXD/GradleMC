package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.*;

public final class RequestSmartAIStatusPacket {
    private static final Map<UUID,Long> LAST_REQUEST=new LinkedHashMap<>();
    private final long epoch;
    private final long requestId;

    public RequestSmartAIStatusPacket(long epoch, long requestId) {
        this.epoch = epoch;
        this.requestId = requestId;
    }

    public static void encode(RequestSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.epoch);
        buffer.writeLong(packet.requestId);
    }

    public static RequestSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
        try {
            if (buffer.readableBytes() != 16) return new RequestSmartAIStatusPacket(0L, 0L);
            long epoch = buffer.readLong();
            long requestId = buffer.readLong();
            return epoch <= 0L || requestId <= 0L ? new RequestSmartAIStatusPacket(0L, 0L) : new RequestSmartAIStatusPacket(epoch, requestId);
        } catch (RuntimeException malformed) {
            return new RequestSmartAIStatusPacket(0L, 0L);
        }
    }

    public static void handle(RequestSmartAIStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null && packet.epoch > 0L && packet.requestId > 0L && allow(sender.getUUID())) {
                NetworkDiagnostics.record("request_status","client-to-server",0);
                // Status is read-only and player-scoped.  Permission gates belong on
                // mutating commands, not on the snapshot needed by an integrated or
                // remote player's dashboard.
                GradleMCNetwork.syncSmartAIStatus(sender, AdaptiveSmartAIManager.statusFor(sender),
                        GuiStatusSnapshot.capture(sender.server, sender.hasPermissions(2)), packet.epoch, packet.requestId);
            } else if(sender!=null) {
                NetworkDiagnostics.ignoredResponse();
            }
        });
        context.setPacketHandled(true);
    }
    public long epoch() { return epoch; }
    public long requestId() { return requestId; }
    private static synchronized boolean allow(UUID player){long now=System.nanoTime(),last=LAST_REQUEST.getOrDefault(player,0L);if(now-last<2_000_000_000L)return false;LAST_REQUEST.put(player,now);while(LAST_REQUEST.size()>256)LAST_REQUEST.remove(LAST_REQUEST.keySet().iterator().next());return true;}
}

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
    public static final RequestSmartAIStatusPacket INSTANCE = new RequestSmartAIStatusPacket();

    private RequestSmartAIStatusPacket() {
    }

    public static void encode(RequestSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(RequestSmartAIStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null && allow(sender.getUUID())) {
                NetworkDiagnostics.record("request_status","client-to-server",0);
                if (sender.hasPermissions(2)) {
                    AdaptiveSmartAIManager.sync(sender);
                } else {
                    GradleMCNetwork.syncSmartAIStatus(sender, AdaptiveSmartAIManager.statusFor(sender), GuiStatusSnapshot.empty());
                }
            } else if(sender!=null) {
                NetworkDiagnostics.ignoredResponse();
            }
        });
        context.setPacketHandled(true);
    }
    private static synchronized boolean allow(UUID player){long now=System.nanoTime(),last=LAST_REQUEST.getOrDefault(player,0L);if(now-last<2_000_000_000L)return false;LAST_REQUEST.put(player,now);while(LAST_REQUEST.size()>256)LAST_REQUEST.remove(LAST_REQUEST.keySet().iterator().next());return true;}
}

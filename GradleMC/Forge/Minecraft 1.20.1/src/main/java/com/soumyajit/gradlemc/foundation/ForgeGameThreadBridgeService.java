package com.soumyajit.gradlemc.foundation;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import java.time.Clock;

/** Forge-only lifecycle adapter; common Foundation code depends only on {@link GameThreadBridge}. */
public final class ForgeGameThreadBridgeService {
    private static volatile MinecraftServer server;
    private static final GameThreadBridge BRIDGE = new GameThreadBridge(Clock.systemUTC(), GameThreadDispatchPolicy.SAFE_DEFAULT,
            new GameThreadBridge.ServerAvailability() {
                @Override public boolean available() { return server != null; }
                @Override public boolean onServerThread() { MinecraftServer current = server; return current != null && current.isSameThread(); }
            });
    private ForgeGameThreadBridgeService() { }
    public static GameThreadBridge bridge() { return BRIDGE; }
    public static void onServerStarted(ServerStartedEvent event) { server = event.getServer(); BRIDGE.startServer(); }
    public static void onServerTick(TickEvent.ServerTickEvent event) { if (event.phase == TickEvent.Phase.END) BRIDGE.drainServerQueue(); }
    public static void onServerStopped(ServerStoppedEvent event) { BRIDGE.stopServer(); server = null; }
}

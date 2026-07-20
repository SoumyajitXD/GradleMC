package com.soumyajit.gradlemc.command;

import com.soumyajit.gradlemc.config.DiagnosticDurationPolicy;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class FpsTestCommandBridge {
    private static final String CLIENT_ONLY_MESSAGE = "FPS testing is client-only. Dedicated servers do not have FPS.";
    private static volatile ClientCommandHandler clientCommandHandler;

    private FpsTestCommandBridge() {
    }

    public static void registerClientHandler(ClientCommandHandler handler) {
        clientCommandHandler = handler;
    }

    public static int start(CommandSourceStack source, int seconds) {
        if (seconds < DiagnosticDurationPolicy.MIN_FPS_SECONDS || seconds > maxSeconds()) {
            source.sendFailure(Component.literal("FPS test duration must be between "
                    + DiagnosticDurationPolicy.MIN_FPS_SECONDS + " and " + maxSeconds() + " seconds."));
            return 0;
        }
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            source.sendFailure(Component.literal(CLIENT_ONLY_MESSAGE));
            return 0;
        }
        if (clientCommandHandler == null) {
            source.sendFailure(Component.literal("FPS testing is not available on this client yet."));
            return 0;
        }
        if (!clientCommandHandler.requestStart(seconds)) {
            source.sendFailure(Component.literal("FPS testing is not available on this client yet."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("FPS test start request sent to this client."), false);
        return 1;
    }

    public static int stop(CommandSourceStack source) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            source.sendFailure(Component.literal(CLIENT_ONLY_MESSAGE));
            return 0;
        }
        if (clientCommandHandler == null) {
            source.sendFailure(Component.literal("FPS testing is not available on this client yet."));
            return 0;
        }
        if (!clientCommandHandler.requestStop()) {
            source.sendFailure(Component.literal("FPS testing is not available on this client yet."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("FPS test stop request sent to this client."), false);
        return 1;
    }

    public interface ClientCommandHandler {
        /** Must enqueue work onto the client thread; common command dispatch can run on a server thread. */
        boolean requestStart(int seconds);

        boolean requestStop();
    }

    public static int maxSeconds() {
        return DiagnosticDurationPolicy.maxFpsSeconds();
    }
}

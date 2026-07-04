package com.soumyajit.gradlemc.command;

import com.soumyajit.gradlemc.config.GradleMCConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class FpsTestCommandBridge {
    public static final int MIN_SECONDS = 5;
    public static final int HARD_MAX_SECONDS = 1800;
    private static final String CLIENT_ONLY_MESSAGE = "FPS testing is client-only. Dedicated servers do not have FPS.";
    private static ClientCommandHandler clientCommandHandler;

    private FpsTestCommandBridge() {
    }

    public static void registerClientHandler(ClientCommandHandler handler) {
        clientCommandHandler = handler;
    }

    public static int start(CommandSourceStack source, int seconds) {
        if (seconds < MIN_SECONDS || seconds > maxSeconds()) {
            source.sendFailure(Component.literal("FPS test duration must be between "
                    + MIN_SECONDS + " and " + maxSeconds() + " seconds."));
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
        return clientCommandHandler.start(source, seconds);
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
        return clientCommandHandler.stop(source);
    }

    public interface ClientCommandHandler {
        int start(CommandSourceStack source, int seconds);

        int stop(CommandSourceStack source);
    }

    public static int maxSeconds() {
        return Math.min(GradleMCConfig.MAX_FPS_TEST_SECONDS.get(), HARD_MAX_SECONDS);
    }
}

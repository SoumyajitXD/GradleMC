package com.soumyajit.gradlemc.check;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.time.Instant;

public record CheckContext(MinecraftServer server, Path reportDirectory, Instant generatedAt) {
}

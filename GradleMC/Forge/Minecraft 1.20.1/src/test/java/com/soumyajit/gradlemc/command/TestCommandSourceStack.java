package com.soumyajit.gradlemc.command;

import com.mojang.brigadier.ResultConsumer;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TaskChainer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.Executor;

/** Minimal real CommandSourceStack for handlers that deliberately need no server or level. */
public final class TestCommandSourceStack extends CommandSourceStack {
    public TestCommandSourceStack(CommandSource source, int permission) {
        super(
            source,
            Vec3.ZERO,
            Vec2.ZERO,
            null,
            permission,
            "test",
            Component.literal("test"),
            null,
            null,
            false,
            (ResultConsumer<CommandSourceStack>) (context, success, result) -> { },
            EntityAnchorArgument.Anchor.FEET,
            CommandSigningContext.ANONYMOUS,
            TaskChainer.immediate((Executor) Runnable::run),
            ignored -> { }
        );
    }
}

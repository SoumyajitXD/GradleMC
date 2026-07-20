package com.soumyajit.gradlemc.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandTreeAuthorizationTest {
    @Test
    void workflowCancellationHasAnExplicitPermissionRequirement() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        GradleMcCommands.register(dispatcher, null, Commands.CommandSelection.ALL);

        var cancel = dispatcher.getRoot().getChild("gradlemc").getChild("workflow").getChild("cancel");
        assertThrows(NullPointerException.class, () -> cancel.getRequirement().test(null),
                "A default Brigadier requirement would accept null; cancellation must invoke its permission predicate.");
    }

    @Test
    void reportDiscoveryHasAnExplicitPermissionRequirement() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        GradleMcCommands.register(dispatcher, null, Commands.CommandSelection.ALL);

        var reports = dispatcher.getRoot().getChild("gradlemc").getChild("reports");
        assertThrows(NullPointerException.class, () -> reports.getRequirement().test(null),
                "Report discovery must invoke its permission predicate instead of accepting every source.");
    }
}

package com.soumyajit.gradlemc.investigation.session;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.foundation.FoundationService;
import com.soumyajit.gradlemc.foundation.TaskCore;
import com.soumyajit.gradlemc.investigation.InvestigationId;
import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import com.soumyajit.gradlemc.investigation.InvestigationRuntimeIdentity;
import com.soumyajit.gradlemc.investigation.planning.InvestigationPlan;
import com.soumyajit.gradlemc.investigation.planning.InvestigationPlanningAdapter;
import com.soumyajit.gradlemc.investigation.planning.InvestigationPlanningContext;
import com.soumyajit.gradlemc.investigation.profile.InvestigationProfileRegistry;
import com.soumyajit.gradlemc.investigation.storage.InvestigationStore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.versions.forge.ForgeVersion;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Server-only command facade.  It creates immutable request data on the command's server thread. */
public final class InvestigationCommandService {
    private static volatile InvestigationSessionService service;

    private InvestigationCommandService() { }

    public static void onServerStarted() {
        InvestigationSessionService old = service;
        if (old != null) old.close();
        service = new InvestigationSessionService(InvestigationPlanningAdapter.builtins(),
                new InvestigationStore(GradleMcPaths.gameDirectory()), Clock.systemUTC());
    }

    public static void onServerStopped() {
        InvestigationSessionService old = service;
        service = null;
        if (old != null) old.close();
    }

    public static CompletableFuture<Suggestions> suggestProfiles(SuggestionsBuilder builder) {
        for (var profile : InvestigationProfileRegistry.builtins().all()) builder.suggest(profile.id().value());
        return builder.buildFuture();
    }

    public static int profiles(CommandSourceStack source) {
        for (var profile : InvestigationProfileRegistry.builtins().all()) {
            source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.profile", profile.id().value()), false);
        }
        return 1;
    }

    public static int plan(CommandSourceStack source, String rawProfile) {
        try {
            InvestigationPlan plan = InvestigationPlanningAdapter.builtins().plan(profile(rawProfile), planningContext());
            source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.plan", plan.profileId().value(), plan.status().name().toLowerCase(), plan.tasks().size()), false);
            return 1;
        } catch (RuntimeException exception) { return invalidProfile(source); }
    }

    public static int start(CommandSourceStack source, String rawProfile) {
        InvestigationSessionService current = require(source);
        if (current == null) return 0;
        try {
            InvestigationSessionSnapshot snapshot = current.start(request(profile(rawProfile)));
            source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.started", snapshot.id().value(), snapshot.profileId().value()), false);
            return 1;
        } catch (IllegalArgumentException exception) { return invalidProfile(source); }
        catch (IllegalStateException exception) { source.sendFailure(Component.translatable("command.gradlemc.investigation.active")); return 0; }
    }

    public static int status(CommandSourceStack source) {
        InvestigationSessionService current = require(source);
        if (current == null) return 0;
        return current.active().map(snapshot -> {
            source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.status", snapshot.id().value(), snapshot.status().name().toLowerCase(), snapshot.completedTasks(), snapshot.totalTasks()), false);
            return 1;
        }).orElseGet(() -> { source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.idle"), false); return 1; });
    }

    public static int cancel(CommandSourceStack source) {
        InvestigationSessionService current = require(source);
        if (current == null) return 0;
        if (!current.cancel()) { source.sendFailure(Component.translatable("command.gradlemc.investigation.no_active")); return 0; }
        source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.cancelled"), false);
        return 1;
    }

    public static int list(CommandSourceStack source, int limit) {
        InvestigationSessionService current = require(source);
        if (current == null) return 0;
        try {
            List<com.soumyajit.gradlemc.investigation.storage.InvestigationIndexEntry> entries = current.list(limit);
            if (entries.isEmpty()) source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.none"), false);
            else entries.forEach(entry -> source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.list", entry.id().value(), entry.profileId().value(), entry.state().name().toLowerCase()), false));
            return 1;
        } catch (IOException exception) { return storageFailure(source); }
    }

    public static int show(CommandSourceStack source, String rawId) {
        InvestigationSessionService current = require(source);
        if (current == null) return 0;
        try {
            var manifest = current.show(new InvestigationId(rawId));
            source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.show", manifest.id().value(), manifest.profileId().value(), manifest.state().name().toLowerCase(), manifest.revision()), false);
            return 1;
        } catch (IllegalArgumentException exception) { source.sendFailure(Component.translatable("command.gradlemc.investigation.invalid_id")); return 0; }
        catch (IOException exception) { return storageFailure(source); }
    }

    public static int recover(CommandSourceStack source) {
        InvestigationSessionService current = require(source);
        if (current == null) return 0;
        try {
            var result = current.recoverInterrupted(16);
            source.sendSuccess(() -> Component.translatable("command.gradlemc.investigation.recovered", result.recovered(), result.skipped()), false);
            return 1;
        } catch (IOException exception) { return storageFailure(source); }
    }

    private static InvestigationSessionRequest request(InvestigationProfileId profile) {
        TaskCore.Context context = FoundationService.captureLiveContext(Clock.systemUTC());
        return new InvestigationSessionRequest(profile, new InvestigationPlanningContext(InvestigationPlanningContext.Side.SERVER, context),
                new InvestigationRuntimeIdentity(GradleMC.CURRENT_VERSION, GradleMC.CURRENT_MINECRAFT_VERSION, ForgeVersion.getVersion(), "SERVER"),
                context.runtimeFingerprint().shortDisplay(), InvestigationSessionPolicy.SAFE_DEFAULT);
    }

    private static InvestigationPlanningContext planningContext() {
        return new InvestigationPlanningContext(InvestigationPlanningContext.Side.SERVER, FoundationService.captureLiveContext(Clock.systemUTC()));
    }

    private static InvestigationProfileId profile(String raw) { return new InvestigationProfileId(raw); }
    private static InvestigationSessionService require(CommandSourceStack source) {
        InvestigationSessionService current = service;
        if (current == null) source.sendFailure(Component.translatable("command.gradlemc.investigation.unavailable"));
        return current;
    }
    private static int invalidProfile(CommandSourceStack source) { source.sendFailure(Component.translatable("command.gradlemc.investigation.invalid_profile")); return 0; }
    private static int storageFailure(CommandSourceStack source) { source.sendFailure(Component.translatable("command.gradlemc.investigation.storage_failure")); return 0; }
}

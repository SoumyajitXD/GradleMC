package com.soumyajit.gradlemc.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.AdaptiveSmartAIManager;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.check.BasicCheckRegistry;
import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.impl.ConfigSanityCheck;
import com.soumyajit.gradlemc.check.impl.RiskRuleCheck;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import com.soumyajit.gradlemc.profiler.GradleMcProfilerService;
import com.soumyajit.gradlemc.report.IssueBundleExporter;
import com.soumyajit.gradlemc.report.Report;
import com.soumyajit.gradlemc.report.ReportFileNames;
import com.soumyajit.gradlemc.report.ReportWriter;
import com.soumyajit.gradlemc.rules.RiskRuleLoader;
import com.soumyajit.gradlemc.rules.RiskRuleSet;
import com.soumyajit.gradlemc.smart.AdaptiveBaseline;
import com.soumyajit.gradlemc.smart.AdaptiveBaselineStore;
import com.soumyajit.gradlemc.smart.AdaptiveThresholds;
import com.soumyajit.gradlemc.smart.DiagnosticEvidence;
import com.soumyajit.gradlemc.smart.DiagnosticFinding;
import com.soumyajit.gradlemc.smart.SmartMetricSnapshots;
import com.soumyajit.gradlemc.smart.SmartRecommendation;
import com.soumyajit.gradlemc.smart.StabilityAdvisor;
import com.soumyajit.gradlemc.smart.StabilityScore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforge.common.NeoForgeVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GradleMcCommands {
    private static final int DEFAULT_ENTITY_RADIUS = 64;
    private static final int MIN_RADIUS = 8;
    private static final int MAX_RADIUS = 512;
    private static final int MIN_PERF_SECONDS = 5;
    private static final int MAX_PERF_SECONDS = 1800;
    private static final int MIN_WORLDGEN_SECONDS = 10;
    private static final int MAX_WORLDGEN_SECONDS = 1800;
    private static final long MIB = 1024L * 1024L;

    private GradleMcCommands() {
    }

    public static boolean hasPermission(CommandSourceStack source) {
        PermissionSet permissions = source.permissions();
        if (permissions == PermissionSet.ALL_PERMISSIONS) {
            return true;
        }
        if (permissions instanceof LevelBasedPermissionSet levelBased) {
            return levelBased.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
        }
        return false;
    }

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("gradlemc")
                .executes(context -> showHelp(context.getSource()))
                .then(Commands.literal("help")
                        .executes(context -> showHelp(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context.getSource())))
                .then(Commands.literal("check")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> runCheck(context.getSource())))
                .then(Commands.literal("config")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> showConfigHelp(context.getSource()))
                        .then(Commands.literal("path")
                                .executes(context -> showConfigPath(context.getSource())))
                        .then(Commands.literal("files")
                                .executes(context -> listConfigFiles(context.getSource())))
                        .then(Commands.literal("check")
                                .executes(context -> runConfigCheck(context.getSource()))))
                .then(Commands.literal("rules")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> showRulesHelp(context.getSource()))
                        .then(Commands.literal("path")
                                .executes(context -> showRulesPath(context.getSource())))
                        .then(Commands.literal("example")
                                .executes(context -> writeRulesExample(context.getSource())))
                        .then(Commands.literal("reload")
                                .executes(context -> reloadRules(context.getSource())))
                        .then(Commands.literal("check")
                                .executes(context -> runRulesCheck(context.getSource()))))
                .then(Commands.literal("mods")
                        .executes(context -> showModsSummary(context.getSource()))
                        .then(Commands.literal("list")
                                .executes(context -> listMods(context.getSource())))
                        .then(Commands.literal("count")
                                .executes(context -> countMods(context.getSource())))
                        .then(Commands.literal("search")
                                .then(Commands.argument("modid", StringArgumentType.word())
                                        .executes(context -> searchMods(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "modid")
                                        )))))
                .then(Commands.literal("perf")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> showPerfHelp(context.getSource()))
                        .then(Commands.literal("start")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(MIN_PERF_SECONDS, MAX_PERF_SECONDS))
                                        .executes(context -> runPerf(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "seconds")
                                        ))))
                        .then(Commands.literal("stop")
                                .executes(context -> PerformanceTestManager.stop(context.getSource())))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(MIN_PERF_SECONDS, MAX_PERF_SECONDS))
                                .executes(context -> runPerf(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds")
                                ))))
                .then(Commands.literal("worldgen")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> showWorldgenHelp(context.getSource()))
                        .then(Commands.literal("start")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(MIN_WORLDGEN_SECONDS, MAX_WORLDGEN_SECONDS))
                                        .executes(context -> startWorldgenObservation(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "seconds")
                                        ))))
                        .then(Commands.literal("stop")
                                .executes(context -> WorldgenObservationManager.stop(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context -> WorldgenObservationManager.status(context.getSource())))
                        .then(Commands.literal("latest")
                                .executes(context -> WorldgenObservationManager.latest(context.getSource()))))
                .then(Commands.literal("profiler")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> showProfilerHelp(context.getSource()))
                        .then(Commands.literal("start")
                                .executes(context -> GradleMcProfilerService.start(
                                        context.getSource(),
                                        GradleMcProfilerService.parseOptions("")
                                ))
                                .then(Commands.argument("options", StringArgumentType.greedyString())
                                        .executes(context -> GradleMcProfilerService.start(
                                                context.getSource(),
                                                GradleMcProfilerService.parseOptions(StringArgumentType.getString(context, "options"))
                                        ))))
                        .then(Commands.literal("stop")
                                .executes(context -> GradleMcProfilerService.stop(context.getSource())))
                        .then(Commands.literal("cancel")
                                .executes(context -> GradleMcProfilerService.cancel(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context -> GradleMcProfilerService.status(context.getSource())))
                        .then(Commands.literal("latest")
                                .executes(context -> GradleMcProfilerService.latest(context.getSource())))
                        .then(Commands.literal("open")
                                .executes(context -> GradleMcProfilerService.open(context.getSource())))
                        .then(Commands.literal("summary")
                                .executes(context -> GradleMcProfilerService.summary(context.getSource())))
                        .then(Commands.literal("export")
                                .executes(context -> GradleMcProfilerService.export(context.getSource()))))
                .then(Commands.literal("memory")
                        .executes(context -> showMemory(context.getSource())))
                .then(Commands.literal("entities")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> countEntities(context.getSource(), defaultEntityRadius()))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(MIN_RADIUS, MAX_RADIUS))
                                .executes(context -> countEntities(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "radius")
                                )))
                        .then(Commands.literal("radius")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(MIN_RADIUS, MAX_RADIUS))
                                        .executes(context -> countEntities(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "radius")
                                        )))))
                .then(Commands.literal("blockentities")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> countBlockEntities(context.getSource(), defaultBlockEntityRadius()))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(MIN_RADIUS, MAX_RADIUS))
                                .executes(context -> countBlockEntities(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "radius")
                                )))
                        .then(Commands.literal("radius")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(MIN_RADIUS, MAX_RADIUS))
                                        .executes(context -> countBlockEntities(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "radius")
                                        )))))
                .then(Commands.literal("files")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> checkFiles(context.getSource())))
                .then(Commands.literal("ai")
                        .executes(context -> showAiHelp(context.getSource()))
                        .then(Commands.literal("status")
                                .executes(context -> AdaptiveSmartAIManager.status(context.getSource())))
                        .then(Commands.literal("reset")
                                .requires(GradleMcCommands::hasPermission)
                                .executes(context -> AdaptiveSmartAIManager.reset(context.getSource()))))
                .then(Commands.literal("smart")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> showSmartHelp(context.getSource()))
                        .then(Commands.literal("score")
                                .executes(context -> smartScore(context.getSource())))
                        .then(Commands.literal("advice")
                                .executes(context -> smartAdvice(context.getSource())))
                        .then(Commands.literal("explain")
                                .executes(context -> smartExplain(context.getSource())))
                        .then(Commands.literal("baseline")
                                .executes(context -> smartBaseline(context.getSource()))
                                .then(Commands.literal("reset")
                                        .executes(context -> smartBaselineReset(context.getSource(), false))
                                        .then(Commands.literal("confirm")
                                                .executes(context -> smartBaselineReset(context.getSource(), true)))))
                        .then(Commands.literal("thresholds")
                                .executes(context -> smartThresholds(context.getSource()))))
                .then(Commands.literal("export")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> exportReport(context.getSource())))
                .then(Commands.literal("issuebundle")
                        .requires(GradleMcCommands::hasPermission)
                        .executes(context -> showIssueBundleHelp(context.getSource()))
                        .then(Commands.literal("create")
                                .executes(context -> createIssueBundle(context.getSource()))))
                .then(Commands.literal("reports")
                        .then(Commands.literal("list")
                                .executes(context -> listReports(context.getSource())))
                        .then(Commands.literal("latest")
                                .executes(context -> latestReport(context.getSource()))))
                .then(Commands.literal("gui")
                        .executes(context -> openGui(context.getSource())))
                .then(Commands.literal("testfps")
                        .then(Commands.literal("start")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(
                                        FpsTestCommandBridge.MIN_SECONDS,
                                        FpsTestCommandBridge.HARD_MAX_SECONDS
                                ))
                                        .executes(context -> FpsTestCommandBridge.start(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "seconds")
                                        ))))
                        .then(Commands.literal("stop")
                                .executes(context -> FpsTestCommandBridge.stop(context.getSource()))))
                .then(Commands.literal("version")
                        .executes(context -> showVersion(context.getSource())));

        event.getDispatcher().register(root);
    }

    private static int showHelp(CommandSourceStack source) {
        send(source, "GradleMC commands:");
        send(source, "General:");
        send(source, "/gradlemc gui - Open the GradleMC panel.");
        send(source, "/gradlemc status - Show a compact diagnostics summary.");
        send(source, "/gradlemc check - Run basic stability checks.");
        send(source, "/gradlemc version - Show GradleMC version info.");
        send(source, "/gradlemc memory - Show memory usage.");
        send(source, "/gradlemc help - Show this help.");
        send(source, "Config and rules:");
        send(source, "/gradlemc config path - Show config path.");
        send(source, "/gradlemc config files - Show config files.");
        send(source, "/gradlemc config check - Validate config.");
        send(source, "/gradlemc rules path - Show rules path.");
        send(source, "/gradlemc rules example - Create/show example rules.");
        send(source, "/gradlemc rules reload - Reload rules.");
        send(source, "/gradlemc rules check - Check active rules.");
        send(source, "Adaptive diagnostics:");
        send(source, "/gradlemc ai status - Show adaptive diagnostics state.");
        send(source, "/gradlemc ai reset - Reset adaptive runtime data. Requires permission level 2.");
        send(source, "/gradlemc smart score - Show current stability score.");
        send(source, "/gradlemc smart advice - Show diagnostic advice.");
        send(source, "/gradlemc smart explain - Explain current evidence.");
        send(source, "/gradlemc smart baseline - Show baseline details.");
        send(source, "/gradlemc smart thresholds - Show adaptive thresholds.");
        send(source, "Reports:");
        send(source, "/gradlemc export - Export diagnostics evidence.");
        send(source, "/gradlemc issuebundle create - Create an issue bundle.");
        send(source, "/gradlemc reports list - List exported reports.");
        send(source, "/gradlemc reports latest - Show the latest report.");
        send(source, "Performance:");
        send(source, "/gradlemc perf start <seconds> - Start a server performance sample.");
        send(source, "/gradlemc perf stop - Stop the active server performance sample.");
        send(source, "/gradlemc testfps start <seconds> - Start a client FPS test.");
        send(source, "/gradlemc testfps stop - Stop the active client FPS test.");
        send(source, "Worldgen:");
        send(source, "/gradlemc worldgen start <seconds> - Observe worldgen/chunk pressure.");
        send(source, "/gradlemc worldgen stop - Stop the active worldgen observation.");
        send(source, "/gradlemc worldgen status - Show current worldgen observation status.");
        send(source, "/gradlemc worldgen latest - Show latest worldgen report.");
        send(source, "Mods and entities:");
        send(source, "/gradlemc mods list - List loaded mods.");
        send(source, "/gradlemc mods count - Count loaded mods.");
        send(source, "/gradlemc mods search <modid> - Search loaded mods by mod ID.");
        send(source, "/gradlemc entities <radius> - Count nearby entities.");
        send(source, "/gradlemc blockentities <radius> - Count nearby block entities.");
        send(source, "/gradlemc files - Show useful GradleMC paths.");
        send(source, "Examples:");
        send(source, "/gradlemc testfps start 30");
        send(source, "/gradlemc worldgen start 60");
        send(source, "/gradlemc perf start 60");
        send(source, "/gradlemc profiler start --mode combined --timeout 60 --interval 20 --thread server");
        send(source, "/gradlemc entities 128");
        send(source, "/gradlemc mods search embeddium");
        return Command.SINGLE_SUCCESS;
    }

    private static int showProfilerHelp(CommandSourceStack source) {
        send(source, "GradleMC profiler commands:");
        send(source, "/gradlemc profiler start - Start a conservative combined local profile.");
        send(source, "/gradlemc profiler start --timeout <seconds> --interval <milliseconds> --thread <name|pattern|*> --only-ticks-over <milliseconds> --include-sleeping true|false --mode tick|cpu-lite|memory-lite|combined");
        send(source, "/gradlemc profiler stop - Stop and write TXT/JSON profile reports.");
        send(source, "/gradlemc profiler cancel - Stop without writing a profile.");
        send(source, "/gradlemc profiler status - Show current profiler status.");
        send(source, "/gradlemc profiler latest - Show the latest profile path.");
        send(source, "/gradlemc profiler summary - Show latest short summary.");
        send(source, "/gradlemc profiler open - Show the local profiles folder.");
        send(source, "/gradlemc profiler export - Confirm the latest local/offline profile path.");
        send(source, "Modes: tick records tick timeline; cpu-lite uses Java stack sampling; memory-lite tracks heap/GC pressure; combined uses safe defaults.");
        send(source, "Memory-lite is not allocation profiling, and CPU-lite is not async-profiler.");
        return Command.SINGLE_SUCCESS;
    }

    private static int openGui(CommandSourceStack source) {
        Optional<ServerPlayer> maybePlayer = playerOrFailure(source, Component.translatable("command.gradlemc.gui.player_only"));
        if (maybePlayer.isEmpty()) {
            return 0;
        }
        GradleMCNetwork.openGui(maybePlayer.get());
        source.sendSuccess(() -> Component.translatable("command.gradlemc.gui.opening"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int showStatus(CommandSourceStack source) {
        send(source, "GradleMC status:");
        send(source, "Product: " + GradleMC.PRODUCT_NAME);
        send(source, "Version: " + gradleMcVersion());
        send(source, "Variant: " + GradleMC.CURRENT_DISPLAY_VARIANT);
        send(source, "Reports: " + enabledLabel(GradleMCConfig.REPORTS_ENABLED.get())
                + ", rule checks: " + enabledLabel(GradleMCConfig.ENABLE_RULE_CHECKS.get())
                + ", smart diagnostics: " + enabledLabel(GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get()));
        send(source, "Adaptive Diagnostics: " + enabledLabel(GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get())
                + ", ambience: " + enabledLabel(GradleMCConfig.ENABLE_ADAPTIVE_AMBIENCE.get())
                + ", events: " + enabledLabel(GradleMCConfig.ENABLE_ADAPTIVE_EVENTS.get()));
        send(source, "GUI: available to in-game clients with /gradlemc gui; console sources cannot open client screens.");
        send(source, "Performance test: " + (PerformanceTestManager.isRunning() ? "running" : "idle")
                + ", worldgen observation: " + (WorldgenObservationManager.isRunning() ? "running" : "idle"));
        send(source, "Technical Stability Score: use /gradlemc smart score for the current diagnostics score.");
        if (source.getEntity() instanceof ServerPlayer player) {
            SmartAIStatus status = AdaptiveSmartAIManager.statusFor(player);
            send(source, "Adaptive Risk: " + status.threatLevel() + " (" + status.threatScore() + "/"
                    + GradleMCConfig.MAX_THREAT_LEVEL.get() + "), top factors: " + status.topRiskFactors());
        } else {
            send(source, "Adaptive Risk: unavailable outside an in-game player context.");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int showVersion(CommandSourceStack source) {
        send(source, GradleMC.PRODUCT_NAME + " v" + gradleMcVersion() + " - " + GradleMC.CURRENT_DISPLAY_VARIANT);
        send(source, "Product: " + GradleMC.PRODUCT_NAME);
        send(source, "Version: " + gradleMcVersion());
        send(source, "Variant: " + GradleMC.CURRENT_DISPLAY_VARIANT);
        send(source, "Minecraft: " + GradleMC.CURRENT_MINECRAFT_VERSION);
        send(source, "Loader: " + GradleMC.CURRENT_LOADER_NAME);
        send(source, "NeoForge: " + NeoForgeVersion.getVersion());
        send(source, "Java: " + System.getProperty("java.version", "unknown"));
        send(source, "Loaded mods: " + ModList.get().getMods().size());
        return Command.SINGLE_SUCCESS;
    }

    private static int runCheck(CommandSourceStack source) {
        Report report = buildReport(source, "GradleMC quick stability check", List.of(
                sideSafetyResult()
        ));
        send(source, "GradleMC check complete: " + report.summaryLine());
        report.results().stream()
                .filter(result -> result.severity() == Severity.WARN
                        || result.severity() == Severity.FAIL
                        || result.severity() == Severity.CRITICAL)
                .limit(5)
                .forEach(result -> send(source, formatResult(result)));
        return Command.SINGLE_SUCCESS;
    }

    private static int showModsSummary(CommandSourceStack source) {
        return countMods(source);
    }

    private static int listMods(CommandSourceStack source) {
        List<IModInfo> mods = sortedMods();
        String preview = mods.stream()
                .limit(12)
                .map(GradleMcCommands::modLabel)
                .collect(Collectors.joining(", "));
        send(source, "Loaded mods: " + mods.size() + (preview.isBlank() ? "" : " (" + preview + ")"));
        if (mods.size() > 12) {
            try {
                Path path = writeModListReport(mods);
                sendPath(source, "Full mod list written to: ", path);
            } catch (IOException exception) {
                source.sendFailure(Component.literal("Could not write full mod list: " + safeMessage(exception)));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int countMods(CommandSourceStack source) {
        send(source, "Loaded mods: " + ModList.get().getMods().size());
        return Command.SINGLE_SUCCESS;
    }

    private static int searchMods(CommandSourceStack source, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<IModInfo> matches = sortedMods().stream()
                .filter(mod -> mod.getModId().toLowerCase(Locale.ROOT).contains(needle)
                        || mod.getDisplayName().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        if (matches.isEmpty()) {
            send(source, "No loaded mod matched: " + query);
            return 0;
        }
        send(source, "Found " + matches.size() + " mod(s):");
        matches.stream().limit(8).forEach(mod -> send(source, modLabel(mod)));
        if (matches.size() > 8) {
            send(source, "More matches omitted. Narrow the search term.");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int showMemory(CommandSourceStack source) {
        MemorySnapshot snapshot = memorySnapshot();
        send(source, snapshot.status() + " memory: used " + snapshot.usedMiB()
                + " MiB, free " + snapshot.freeMiB()
                + " MiB, total " + snapshot.totalMiB()
                + " MiB, max " + snapshot.maxMiB() + " MiB");
        return Command.SINGLE_SUCCESS;
    }

    private static int runPerf(CommandSourceStack source, int seconds) {
        if (seconds > maxPerfSeconds()) {
            source.sendFailure(Component.literal("Performance duration must be between 5 and " + maxPerfSeconds() + " seconds."));
            return 0;
        }
        return PerformanceTestManager.start(source, seconds);
    }

    private static int showPerfHelp(CommandSourceStack source) {
        send(source, "GradleMC performance commands:");
        send(source, "/gradlemc perf start <seconds> - Start a server TPS/MSPT sample.");
        send(source, "/gradlemc perf stop - Stop the active server performance sample.");
        send(source, "Compatibility alias: /gradlemc perf <seconds>.");
        return Command.SINGLE_SUCCESS;
    }

    private static int showWorldgenHelp(CommandSourceStack source) {
        send(source, "GradleMC worldgen commands: /gradlemc worldgen start <seconds>|stop|status|latest");
        send(source, "Observation is passive: no forced chunks, no teleports, no unloaded chunk scans.");
        return WorldgenObservationManager.status(source);
    }

    private static int startWorldgenObservation(CommandSourceStack source, int seconds) {
        if (seconds > maxWorldgenSeconds()) {
            source.sendFailure(Component.literal("Worldgen observation duration must be between 10 and "
                    + maxWorldgenSeconds() + " seconds."));
            return 0;
        }
        return WorldgenObservationManager.start(source, seconds);
    }

    private static int showIssueBundleHelp(CommandSourceStack source) {
        send(source, "GradleMC issue bundle commands: /gradlemc issuebundle create");
        send(source, "Bundles include GradleMC reports and summaries, not full logs, crash reports, configs, or mods.");
        return Command.SINGLE_SUCCESS;
    }

    private static int createIssueBundle(CommandSourceStack source) {
        if (!GradleMCConfig.ISSUE_BUNDLE_ENABLED.get()) {
            source.sendFailure(Component.literal("Issue bundles are disabled in gradlemc-common.toml."));
            return 0;
        }
        try {
            Path path = new IssueBundleExporter().create(source.getServer());
            sendPath(source, "GradleMC issue bundle created: ", path);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Issue bundle export failed: " + safeMessage(exception)));
            return 0;
        }
    }

    private static int countEntities(CommandSourceStack source, int radius) {
        if (!validRadius(source, radius)) {
            return 0;
        }
        Optional<ServerPlayer> maybePlayer = playerOrFailure(source);
        if (maybePlayer.isEmpty()) {
            return 0;
        }
        ServerPlayer player = maybePlayer.get();
        AABB bounds = player.getBoundingBox().inflate(radius);
        List<Entity> entities = player.level().getEntities(player, bounds);
        Map<String, Long> counts = entities.stream()
                .collect(Collectors.groupingBy(GradleMcCommands::entityName, Collectors.counting()));
        send(source, "Entities within " + radius + " blocks: " + entities.size());
        topCounts(counts, 8).forEach(line -> send(source, line));
        SmartMetricSnapshots.recordEntityScan(radius, entities.size());
        return Command.SINGLE_SUCCESS;
    }

    private static int countBlockEntities(CommandSourceStack source, int radius) {
        if (!validRadius(source, radius)) {
            return 0;
        }
        Optional<ServerPlayer> maybePlayer = playerOrFailure(source);
        if (maybePlayer.isEmpty()) {
            return 0;
        }
        ServerPlayer player = maybePlayer.get();
        ServerLevel level = player.level();
        BlockPos center = player.blockPosition();
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        Map<String, Long> counts = new java.util.HashMap<>();
        int total = 0;
        int loadedChunks = 0;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkAccess chunkAccess = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(chunkAccess instanceof LevelChunk chunk)) {
                    continue;
                }
                loadedChunks++;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity == null || !withinRadius(center, blockEntity.getBlockPos(), radius)) {
                        continue;
                    }
                    total++;
                    counts.merge(blockEntityName(blockEntity), 1L, Long::sum);
                }
            }
        }

        Severity severity = total >= 512 ? Severity.WARN : Severity.PASS;
        send(source, severity + " block entities within " + radius + " blocks: " + total
                + " across " + loadedChunks + " loaded chunk(s).");
        topCounts(counts, 8).forEach(line -> send(source, line));
        SmartMetricSnapshots.recordBlockEntityScan(radius, total);
        return Command.SINGLE_SUCCESS;
    }

    private static int checkFiles(CommandSourceStack source) {
        CheckResult reportStatus = reportDirectoryStatus();
        send(source, formatResult(reportStatus));
        Path configDir = GradleMcPaths.configDirectory();
        send(source, "Config directory: " + configDir + " exists=" + Files.isDirectory(configDir)
                + " readable=" + Files.isReadable(configDir));
        Path latestLog = FMLPaths.GAMEDIR.get().resolve("logs").resolve("latest.log").normalize();
        boolean logVisible = latestLog.startsWith(FMLPaths.GAMEDIR.get().normalize()) && Files.isRegularFile(latestLog);
        send(source, "Latest log path: " + (logVisible ? latestLog.toString() : "not found"));
        return reportStatus.severity() == Severity.FAIL ? 0 : Command.SINGLE_SUCCESS;
    }

    private static int showConfigHelp(CommandSourceStack source) {
        send(source, "GradleMC config commands: /gradlemc config path|files|check");
        send(source, "Config directory: " + GradleMcPaths.configDirectory());
        send(source, "Reports: " + (GradleMCConfig.REPORTS_ENABLED.get() ? "enabled" : "disabled")
                + ", rules: " + (GradleMCConfig.ENABLE_RULE_CHECKS.get() ? "enabled" : "disabled"));
        return Command.SINGLE_SUCCESS;
    }

    private static int showAiHelp(CommandSourceStack source) {
        send(source, "GradleMC adaptive diagnostics commands: /gradlemc ai status, /gradlemc ai reset");
        send(source, "/gradlemc ai reset requires permission level 2.");
        send(source, "Runtime-only local player signals. No cloud AI, LLMs, external APIs, or telemetry.");
        return Command.SINGLE_SUCCESS;
    }

    private static int showConfigPath(CommandSourceStack source) {
        send(source, "Minecraft config directory: " + GradleMcPaths.configDirectory());
        send(source, "GradleMC config: " + GradleMcPaths.configDirectory().resolve("gradlemc-common.toml").normalize());
        send(source, "GradleMC output root: " + GradleMcPaths.displayPath(GradleMcPaths.gradleMcDirectory()));
        return Command.SINGLE_SUCCESS;
    }

    private static int listConfigFiles(CommandSourceStack source) {
        long count = ConfigSanityCheck.countConfigFiles();
        List<Path> files = ConfigSanityCheck.listConfigFiles(Math.min(maxReportsListed(), 20));
        send(source, "Config files in top-level config directory: " + count);
        files.forEach(path -> send(source, path.getFileName().toString()));
        if (count > files.size()) {
            send(source, "More files omitted. Use /gradlemc export for a report summary.");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runConfigCheck(CommandSourceStack source) {
        CheckContext context = new CheckContext(source.getServer(), GradleMcPaths.reportDirectory(), Instant.now());
        List<CheckResult> results = new ConfigSanityCheck().run(context);
        sendResultSummary(source, "Config check", results);
        results.stream()
                .filter(GradleMcCommands::importantResult)
                .limit(6)
                .forEach(result -> send(source, formatResult(result)));
        return Command.SINGLE_SUCCESS;
    }

    private static int showRulesHelp(CommandSourceStack source) {
        RiskRuleSet rules = RiskRuleLoader.current();
        send(source, "GradleMC rules commands: /gradlemc rules path|example|reload|check");
        send(source, "Rule file: " + rules.path());
        send(source, "Loaded rules: " + rules.rules().size()
                + ", enabled=" + GradleMCConfig.ENABLE_RULE_CHECKS.get());
        return Command.SINGLE_SUCCESS;
    }

    private static int showRulesPath(CommandSourceStack source) {
        send(source, "GradleMC rule file: " + RiskRuleLoader.rulesPath());
        send(source, "Example rule file: " + RiskRuleLoader.examplePath());
        return Command.SINGLE_SUCCESS;
    }

    private static int writeRulesExample(CommandSourceStack source) {
        try {
            boolean written = RiskRuleLoader.writeExampleIfMissing();
            send(source, (written ? "Example rule file written: " : "Example rule file already exists: ")
                    + RiskRuleLoader.examplePath());
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Could not write example rule file: " + safeMessage(exception)));
            return 0;
        }
    }

    private static int reloadRules(CommandSourceStack source) {
        RiskRuleSet rules = RiskRuleLoader.reload();
        send(source, "Risk rules reloaded: " + rules.rules().size() + " valid rule(s).");
        rules.loadResults().stream()
                .filter(GradleMcCommands::importantResult)
                .limit(5)
                .forEach(result -> send(source, formatResult(result)));
        return Command.SINGLE_SUCCESS;
    }

    private static int runRulesCheck(CommandSourceStack source) {
        CheckContext context = new CheckContext(source.getServer(), GradleMcPaths.reportDirectory(), Instant.now());
        List<CheckResult> results = new RiskRuleCheck().run(context);
        sendResultSummary(source, "Rules check", results);
        results.stream()
                .filter(GradleMcCommands::importantResult)
                .limit(8)
                .forEach(result -> send(source, formatResult(result)));
        return Command.SINGLE_SUCCESS;
    }

    private static int showSmartHelp(CommandSourceStack source) {
        if (!GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get()) {
            send(source, "Smart diagnostics are disabled in gradlemc-common.toml.");
            return 0;
        }
        AdaptiveBaseline baseline = AdaptiveBaselineStore.load();
        send(source, "GradleMC smart diagnostics: local rule-based/adaptive diagnostics only.");
        send(source, "/gradlemc smart score|advice|explain|baseline|thresholds");
        send(source, "Baseline: " + (baseline.isEmpty() ? "none yet" : baseline.metrics().size() + " metric(s), last updated " + baseline.lastUpdated()));
        send(source, "No LLMs, cloud AI, external APIs, or telemetry.");
        return Command.SINGLE_SUCCESS;
    }

    private static int smartScore(CommandSourceStack source) {
        if (!GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get()) {
            send(source, "Smart diagnostics are disabled in gradlemc-common.toml.");
            return 0;
        }
        AdaptiveBaselineStore.updateMemorySnapshot();
        Report report = buildReport(source, "GradleMC smart stability score", List.of(
                sideSafetyResult(),
                latestFpsReportResult(),
                latestPerfReportResult(),
                latestWorldgenReportResult()
        ));
        StabilityScore score = StabilityAdvisor.evaluate(source.getServer(), report.results());
        send(source, score.summaryLine());
        if (!score.findings().isEmpty()) {
            send(source, "Top risks: " + score.findings().stream()
                    .limit(3)
                    .map(DiagnosticFinding::title)
                    .collect(Collectors.joining(", ")));
        }
        if (!score.missingDataNotes().isEmpty()) {
            send(source, "Missing data lowers confidence: " + score.missingDataNotes().stream().limit(2).collect(Collectors.joining("; ")));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int smartAdvice(CommandSourceStack source) {
        if (!GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get()) {
            send(source, "Smart diagnostics are disabled in gradlemc-common.toml.");
            return 0;
        }
        StabilityScore score = currentSmartScore(source);
        send(source, score.summaryLine());
        List<SmartRecommendation> recommendations = score.recommendations().stream()
                .limit(GradleMCConfig.SMART_ADVICE_MAX_ITEMS.get())
                .toList();
        if (recommendations.isEmpty()) {
            send(source, "No smart recommendations from available data.");
            return Command.SINGLE_SUCCESS;
        }
        for (SmartRecommendation recommendation : recommendations) {
            send(source, recommendation.title() + ": " + recommendation.action());
        }
        send(source, "Use /gradlemc smart explain or /gradlemc export for evidence details.");
        return Command.SINGLE_SUCCESS;
    }

    private static int smartExplain(CommandSourceStack source) {
        if (!GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get()) {
            send(source, "Smart diagnostics are disabled in gradlemc-common.toml.");
            return 0;
        }
        StabilityScore score = currentSmartScore(source);
        send(source, score.summaryLine());
        if (score.findings().isEmpty()) {
            send(source, "No smart findings from available data.");
        } else {
            for (DiagnosticFinding finding : score.findings().stream().limit(5).toList()) {
                send(source, "[" + finding.severity() + "] " + finding.title() + " (confidence " + finding.confidence() + ")");
                finding.evidence().stream().limit(2).map(GradleMcCommands::formatEvidence).forEach(line -> send(source, line));
            }
        }
        if (!score.trendNotes().isEmpty()) {
            send(source, "Trend notes: " + score.trendNotes().stream().limit(2).collect(Collectors.joining("; ")));
        }
        if (!score.missingDataNotes().isEmpty()) {
            send(source, "Missing data: " + score.missingDataNotes().stream().limit(3).collect(Collectors.joining("; ")));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int smartBaseline(CommandSourceStack source) {
        AdaptiveBaseline baseline = AdaptiveBaselineStore.load();
        sendPath(source, "Adaptive baseline file: ", AdaptiveBaselineStore.path());
        if (baseline.isEmpty()) {
            send(source, "No adaptive baseline yet. Run /gradlemc perf start 60, /gradlemc testfps start 60, /gradlemc worldgen start 120, /gradlemc entities 64, or /gradlemc blockentities 64 naturally.");
            return 0;
        }
        send(source, "Last updated: " + baseline.lastUpdated());
        baseline.metrics().forEach((name, stats) -> send(source, name + ": samples=" + stats.samples()
                + ", avg=" + format(stats.average())
                + ", min=" + format(stats.min())
                + ", max=" + format(stats.max())));
        return Command.SINGLE_SUCCESS;
    }

    private static int smartBaselineReset(CommandSourceStack source, boolean confirmed) {
        if (!confirmed) {
            source.sendFailure(Component.literal("This deletes only " + GradleMcPaths.displayPath(AdaptiveBaselineStore.path())
                    + ". Run /gradlemc smart baseline reset confirm to continue."));
            return 0;
        }
        try {
            boolean deleted = AdaptiveBaselineStore.reset();
            send(source, deleted ? "Adaptive baseline reset." : "No adaptive baseline file existed.");
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Could not reset adaptive baseline: " + safeMessage(exception)));
            return 0;
        }
    }

    private static int smartThresholds(CommandSourceStack source) {
        if (!GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get()) {
            send(source, "Smart diagnostics are disabled in gradlemc-common.toml.");
            return 0;
        }
        AdaptiveBaseline baseline = AdaptiveBaselineStore.load();
        AdaptiveThresholds.describe(baseline).forEach(line -> send(source, line));
        return Command.SINGLE_SUCCESS;
    }

    private static StabilityScore currentSmartScore(CommandSourceStack source) {
        Report report = buildReport(source, "GradleMC smart diagnostics", List.of(
                sideSafetyResult(),
                latestFpsReportResult(),
                latestPerfReportResult(),
                latestWorldgenReportResult()
        ));
        return StabilityAdvisor.evaluate(source.getServer(), report.results());
    }

    private static int exportReport(CommandSourceStack source) {
        if (!GradleMCConfig.REPORTS_ENABLED.get()) {
            source.sendFailure(Component.literal("GradleMC reports are disabled in gradlemc-common.toml."));
            return 0;
        }
        Report report = buildReport(source, "GradleMC exported diagnostics", List.of(
                sideSafetyResult(),
                latestFpsReportResult(),
                latestPerfReportResult(),
                latestWorldgenReportResult()
        ));
        try {
            Path path = new ReportWriter().write(report, GradleMcPaths.exportDirectory());
            sendPath(source, "GradleMC diagnostics export written: ", path);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("GradleMC export failed: " + safeMessage(exception)));
            return 0;
        }
    }

    private static int listReports(CommandSourceStack source) {
        List<Path> reports = recentReports();
        if (reports.isEmpty()) {
            send(source, "No GradleMC reports found under " + GradleMcPaths.displayPath(GradleMcPaths.gradleMcDirectory())
                    + " or legacy config/gradlemc/reports.");
            return 0;
        }
        send(source, "Recent GradleMC reports:");
        reports.stream().limit(maxReportsListed()).forEach(path -> send(source, GradleMcPaths.displayPath(path)));
        if (legacyReportsAvailable()) {
            send(source, "Legacy reports found under config/gradlemc/reports; new writes use gradlemc/.");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int latestReport(CommandSourceStack source) {
        List<Path> reports = recentReports();
        if (reports.isEmpty()) {
            send(source, "No GradleMC reports found under " + GradleMcPaths.displayPath(GradleMcPaths.gradleMcDirectory())
                    + " or legacy config/gradlemc/reports.");
            return 0;
        }
        Path latest = reports.get(0);
        sendPath(source, "Latest GradleMC report: ", latest);
        try (Stream<String> lines = Files.lines(latest, StandardCharsets.UTF_8)) {
            lines.filter(line -> line.startsWith("Summary:")
                            || line.startsWith("Stability Score:")
                            || line.startsWith("Technical Stability Score:")
                            || line.startsWith("Average FPS:")
                            || line.startsWith("Approximate TPS:")
                            || line.startsWith("Loaded chunks start/end:")
                            || line.startsWith("End reason:"))
                    .limit(3)
                    .forEach(line -> send(source, line));
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Could not read latest report summary: " + safeMessage(exception)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static Report buildReport(CommandSourceStack source, String title, List<CheckResult> additionalResults) {
        Instant generatedAt = Instant.now();
        CheckContext checkContext = new CheckContext(source.getServer(), GradleMcPaths.reportDirectory(), generatedAt);
        List<CheckResult> results = new ArrayList<>(BasicCheckRegistry.runDefaultChecks(checkContext));
        results.addAll(additionalResults);
        return new Report(title, generatedAt, results);
    }

    private static CheckResult fileStatusResult() {
        CheckResult reportStatus = reportDirectoryStatus();
        Path configDir = GradleMcPaths.configDirectory();
        String detail = "Config directory " + configDir + " readable=" + Files.isReadable(configDir)
                + "; output root " + GradleMcPaths.gradleMcDirectory()
                + "; report directory " + GradleMcPaths.reportDirectory();
        return CheckResult.of(
                reportStatus.severity(),
                CheckCategory.FILES,
                "File access status",
                detail,
                reportStatus.suggestion()
        );
    }

    private static CheckResult sideSafetyResult() {
        return CheckResult.of(
                Severity.PASS,
                CheckCategory.CONFIG,
                "Side-safety guard active",
                "Common commands use the FPS bridge and do not directly load client-only Minecraft classes.",
                "Keep future client diagnostics behind Dist.CLIENT entry points."
        );
    }

    private static CheckResult latestFpsReportResult() {
        return recentReports().stream()
                .filter(path -> path.getFileName().toString().startsWith("gradlemc-fps-test-"))
                .findFirst()
                .map(path -> CheckResult.of(
                        Severity.INFO,
                        CheckCategory.PERFORMANCE,
                        "Latest FPS report",
                        path.getFileName().toString(),
                        "Use /gradlemc reports latest for a short summary."
                ))
                .orElseGet(() -> CheckResult.of(
                        Severity.INFO,
                        CheckCategory.PERFORMANCE,
                        "No FPS test report found",
                        "FPS testing is optional and client-only.",
                        "Run /gradlemc testfps start <seconds> on a client if FPS data is needed."
                ));
    }

    private static CheckResult latestPerfReportResult() {
        Path path = PerformanceTestManager.latestReportPath();
        if (path != null) {
            return CheckResult.of(
                    Severity.INFO,
                    CheckCategory.PERFORMANCE,
                    "Latest performance test report",
                    path.getFileName().toString(),
                    "Use /gradlemc perf start <seconds> for a fresh bounded TPS/MSPT sample."
            );
        }
        return CheckResult.of(
                Severity.INFO,
                CheckCategory.PERFORMANCE,
                "No performance test report found",
                "Performance sampling is optional and server-side.",
                "Run /gradlemc perf start <seconds> if TPS/MSPT context is needed."
        );
    }

    private static CheckResult latestWorldgenReportResult() {
        Path path = WorldgenObservationManager.latestReportPath();
        if (path != null) {
            return CheckResult.of(
                    Severity.INFO,
                    CheckCategory.WORLDGEN,
                    "Latest worldgen observation report",
                    path.getFileName().toString(),
                    "Use /gradlemc worldgen latest for a short summary."
            );
        }
        return CheckResult.of(
                Severity.INFO,
                CheckCategory.WORLDGEN,
                "No worldgen observation report found",
                "Worldgen pressure observation is passive and optional.",
                "Run /gradlemc worldgen start <seconds> while exploring if chunk pressure context is needed."
        );
    }

    private static CheckResult reportDirectoryStatus() {
        if (!GradleMCConfig.REPORTS_ENABLED.get()) {
            return CheckResult.of(
                    Severity.INFO,
                    CheckCategory.FILES,
                    "Report exports disabled",
                    "reportsEnabled=false in GradleMC config.",
                    "Enable reports if text exports are needed."
            );
        }
        try {
            Files.createDirectories(GradleMcPaths.reportDirectory());
            boolean writable = Files.isWritable(GradleMcPaths.reportDirectory());
            return CheckResult.of(
                    writable ? Severity.PASS : Severity.WARN,
                    CheckCategory.FILES,
                    "Report directory " + (writable ? "is writable" : "is not writable"),
                    GradleMcPaths.reportDirectory().toString(),
                    writable ? "Reports can be exported here." : "Check permissions for the gradlemc output folder."
            );
        } catch (IOException exception) {
            return CheckResult.of(
                    Severity.FAIL,
                    CheckCategory.FILES,
                    "Report directory could not be prepared",
                    safeMessage(exception),
                    "Check permissions for the gradlemc output folder."
            );
        }
    }

    private static Optional<ServerPlayer> playerOrFailure(CommandSourceStack source) {
        return playerOrFailure(source, Component.literal("Player context required. Run this command as an in-game player."));
    }

    private static Optional<ServerPlayer> playerOrFailure(CommandSourceStack source, Component failureMessage) {
        try {
            return Optional.of(source.getPlayerOrException());
        } catch (Exception exception) {
            source.sendFailure(failureMessage);
            return Optional.empty();
        }
    }

    private static MemorySnapshot memorySnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory() / MIB;
        long total = runtime.totalMemory() / MIB;
        long free = runtime.freeMemory() / MIB;
        long used = total - free;
        double pressure = max <= 0 ? 0.0D : (double) used / max;
        Severity status = pressure >= 0.95D ? Severity.CRITICAL : pressure >= 0.80D ? Severity.WARN : Severity.PASS;
        return new MemorySnapshot(used, free, total, max, status);
    }

    private static List<IModInfo> sortedMods() {
        return ModList.get().getMods().stream()
                .sorted(Comparator.comparing(IModInfo::getModId))
                .toList();
    }

    private static Path writeModListReport(List<IModInfo> mods) throws IOException {
        Files.createDirectories(GradleMcPaths.reportDirectory());
        Path path = uniqueReportPath("gradlemc-mods-", Instant.now());
        List<String> lines = new ArrayList<>();
        lines.add("GradleMC Loaded Mods");
        lines.add("====================");
        lines.add("Loaded mods: " + mods.size());
        lines.add("");
        mods.forEach(mod -> lines.add(modLabel(mod)));
        Files.write(path, lines, StandardCharsets.UTF_8);
        return path;
    }

    private static Path uniqueReportPath(String prefix, Instant instant) {
        return ReportFileNames.unique(GradleMcPaths.reportDirectory(), prefix, instant, ".txt");
    }

    private static List<Path> recentReports() {
        return GradleMcPaths.reportSearchDirectories().stream()
                .filter(Files::isDirectory)
                .flatMap(GradleMcCommands::safeList)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith("gradlemc-"))
                .sorted((left, right) -> {
                    try {
                        return Files.getLastModifiedTime(right).compareTo(Files.getLastModifiedTime(left));
                    } catch (IOException exception) {
                        return right.getFileName().toString().compareTo(left.getFileName().toString());
                    }
                })
                .limit(maxReportsListed())
                .toList();
    }

    private static Stream<Path> safeList(Path directory) {
        try {
            return Files.list(directory);
        } catch (IOException exception) {
            return Stream.empty();
        }
    }

    private static boolean legacyReportsAvailable() {
        Path legacy = GradleMcPaths.legacyReportDirectory();
        if (!Files.isDirectory(legacy)) {
            return false;
        }
        try (Stream<Path> paths = Files.list(legacy)) {
            return paths.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().startsWith("gradlemc-"));
        } catch (IOException exception) {
            return false;
        }
    }

    private static List<String> topCounts(Map<String, Long> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .toList();
    }

    private static String entityName(Entity entity) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? entity.getType().toString() : key.toString();
    }

    private static String blockEntityName(BlockEntity blockEntity) {
        Identifier key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        return key == null ? blockEntity.getType().toString() : key.toString();
    }

    private static boolean withinRadius(BlockPos center, BlockPos pos, int radius) {
        return Math.abs(pos.getX() - center.getX()) <= radius
                && Math.abs(pos.getY() - center.getY()) <= radius
                && Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    private static String modLabel(IModInfo mod) {
        return mod.getModId() + " (" + mod.getDisplayName() + ") " + mod.getVersion();
    }

    private static String gradleMcVersion() {
        return ModList.get().getModContainerById(GradleMC.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    private static int defaultEntityRadius() {
        return Math.min(GradleMCConfig.DEFAULT_ENTITY_SCAN_RADIUS.get(), maxScanRadius());
    }

    private static int defaultBlockEntityRadius() {
        return Math.min(GradleMCConfig.DEFAULT_BLOCK_ENTITY_SCAN_RADIUS.get(), maxScanRadius());
    }

    private static int maxScanRadius() {
        return GradleMCConfig.MAX_SCAN_RADIUS.get();
    }

    private static int maxPerfSeconds() {
        return GradleMCConfig.MAX_PERF_SECONDS.get();
    }

    private static int maxWorldgenSeconds() {
        return GradleMCConfig.MAX_WORLDGEN_OBSERVATION_SECONDS.get();
    }

    private static int maxReportsListed() {
        return GradleMCConfig.MAX_REPORT_FILES_LISTED.get();
    }

    private static boolean validRadius(CommandSourceStack source, int radius) {
        int max = maxScanRadius();
        if (radius < MIN_RADIUS || radius > max) {
            source.sendFailure(Component.literal("Scan radius must be between " + MIN_RADIUS + " and " + max + " blocks."));
            return false;
        }
        return true;
    }

    private static boolean importantResult(CheckResult result) {
        return result.severity() == Severity.WARN
                || result.severity() == Severity.FAIL
                || result.severity() == Severity.CRITICAL
                || GradleMCConfig.VERBOSE_CHAT_OUTPUT.get();
    }

    private static void sendResultSummary(CommandSourceStack source, String label, List<CheckResult> results) {
        long pass = results.stream().filter(result -> result.severity() == Severity.PASS).count();
        long info = results.stream().filter(result -> result.severity() == Severity.INFO).count();
        long warn = results.stream().filter(result -> result.severity() == Severity.WARN).count();
        long fail = results.stream()
                .filter(result -> result.severity() == Severity.FAIL || result.severity() == Severity.CRITICAL)
                .count();
        send(source, label + ": " + pass + " pass, " + info + " info, " + warn + " warn, " + fail + " fail");
    }

    private static String formatResult(CheckResult result) {
        return "[" + result.severity() + "] " + result.title() + ": " + result.detail();
    }

    private static String formatEvidence(DiagnosticEvidence evidence) {
        return evidence.metric() + ": observed " + evidence.observed() + ", threshold " + evidence.threshold();
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String enabledLabel(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static void sendPath(CommandSourceStack source, String prefix, Path path) {
        source.sendSuccess(() -> GradleMcPaths.pathComponent(prefix, path), false);
    }

    private record MemorySnapshot(long usedMiB, long freeMiB, long totalMiB, long maxMiB, Severity status) {
    }
}

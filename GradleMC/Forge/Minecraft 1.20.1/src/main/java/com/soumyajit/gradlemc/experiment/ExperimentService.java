package com.soumyajit.gradlemc.experiment;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.instance.*;
import com.soumyajit.gradlemc.modaudit.ModDescriptor;
import com.soumyajit.gradlemc.task.*;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.versions.forge.ForgeVersion;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class ExperimentService {
    private ExperimentService() { }
    private static ExperimentStore store() { return new ExperimentStore(GradleMcPaths.experimentDirectory()); }

    public static int create(CommandSourceStack source, String name, String workflow) {
        if (!DiagnosticRunService.workflows().contains(workflow) && DiagnosticRunService.tasks().stream().noneMatch(task -> task.id().equals(workflow))) return fail(source, "Unknown task or workflow: " + workflow);
        try {
            ExperimentRecord record = store().create(name, workflow);
            send(source, "Created experiment " + record.name() + " for " + record.workflow() + ". Run /gradlemc experiment baseline " + name + ".");
            return 1;
        } catch (IOException | IllegalArgumentException exception) { return fail(source, "Could not create experiment: " + safe(exception)); }
    }

    public static int baseline(CommandSourceStack source, String name) {
        try {
            ExperimentRecord record = required(name);
            if (record.cancelled()) return fail(source, "Experiment is cancelled.");
            if (DiagnosticRunService.run(source, record.workflow(), false, true) == 0) return fail(source, "Baseline workflow did not complete successfully.");
            ExperimentRecord updated = new ExperimentRecord(record.name(), record.workflow(), record.createdAt(), false,
                    snapshot(record.workflow()), null, record.manualAction(), record.rollback());
            store().write(updated);
            send(source, "Baseline recorded. " + updated.manualAction());
            send(source, "After the change, run /gradlemc experiment compare " + name + ". Rollback: " + updated.rollback());
            return 1;
        } catch (IOException | IllegalArgumentException exception) { return fail(source, "Could not record baseline: " + safe(exception)); }
    }

    public static int compare(CommandSourceStack source, String name) {
        try {
            ExperimentRecord record = required(name);
            if (record.cancelled()) return fail(source, "Experiment is cancelled.");
            if (record.baseline() == null) return fail(source, "Record a baseline first.");
            if (DiagnosticRunService.run(source, record.workflow(), false, true) == 0) return fail(source, "Candidate workflow did not complete successfully.");
            ExperimentSnapshot candidate = snapshot(record.workflow());
            ExperimentComparison comparison = ExperimentComparator.compare(record.baseline(), candidate);
            store().write(new ExperimentRecord(record.name(), record.workflow(), record.createdAt(), false,
                    record.baseline(), candidate, record.manualAction(), record.rollback()));
            send(source, "Experiment " + name + ": " + comparison.verdict() + " - " + comparison.explanation());
            if (!comparison.comparabilityIssues().isEmpty()) send(source, "Comparability: " + comparison.comparabilityIssues());
            send(source, "Normalized metric changes: " + comparison.relativeChanges());
            send(source, "Rollback: " + record.rollback());
            return comparison.verdict() == ExperimentVerdict.REGRESSED ? 0 : 1;
        } catch (IOException | IllegalArgumentException exception) { return fail(source, "Could not compare experiment: " + safe(exception)); }
    }

    public static int status(CommandSourceStack source, String name) {
        try {
            ExperimentRecord record = required(name);
            send(source, record.name() + " | workflow=" + record.workflow() + " | " + (record.cancelled() ? "CANCELLED" : record.baseline() == null ? "AWAITING_BASELINE" : record.candidate() == null ? "AWAITING_CHANGE" : "COMPARED"));
            send(source, "Action: " + record.manualAction() + " Rollback: " + record.rollback()); return 1;
        } catch (IOException | IllegalArgumentException exception) { return fail(source, "Could not inspect experiment: " + safe(exception)); }
    }

    public static int cancel(CommandSourceStack source, String name) {
        try {
            ExperimentRecord record = required(name);
            store().write(new ExperimentRecord(record.name(), record.workflow(), record.createdAt(), true, record.baseline(), record.candidate(), record.manualAction(), record.rollback()));
            send(source, "Experiment " + name + " cancelled; stored evidence was retained for auditability."); return 1;
        } catch (IOException | IllegalArgumentException exception) { return fail(source, "Could not cancel experiment: " + safe(exception)); }
    }

    public static int list(CommandSourceStack source) {
        try { List<ExperimentRecord> records=store().list(); if(records.isEmpty()) send(source,"No controlled experiments found."); else records.forEach(record -> send(source,record.name()+" | "+record.workflow()+" | "+record.createdAt())); return 1; }
        catch(IOException exception){return fail(source,"Could not list experiments: "+safe(exception));}
    }

    private static ExperimentRecord required(String name) throws IOException { return store().get(name).orElseThrow(() -> new IllegalArgumentException("Unknown experiment: " + name)); }
    private static ExperimentSnapshot snapshot(String workflow) {
        MinecraftInstanceSnapshot instance = MinecraftInstanceService.current(true);
        Map<String,String> context = new TreeMap<>(); context.put("minecraft", GradleMC.CURRENT_MINECRAFT_VERSION); context.put("forge", ForgeVersion.getVersion());
        context.put("javaMajor", Integer.toString(Runtime.version().feature())); context.put("workflow", workflow); context.put("world", instance.world().availability().name());
        context.put("mods", instance.mods().value().mods().stream().map(ModDescriptor::normalizedIdentifier).sorted().collect(Collectors.joining(",")));
        context.put("resourcePacks", instance.resourcePacks().value().stream().map(PackDescriptor::id).collect(Collectors.joining(",")));
        context.put("shaderState", instance.shaderPacks().availability().name()); context.put("datapacks", instance.dataPacks().availability().name());
        context.put("graphicsSettings", "unavailable"); context.put("taskParameters", workflow); context.put("testDuration", duration(DiagnosticRunService.latestResults()));
        Map<String,Double> metrics = new TreeMap<>();
        Set<String> allowed = Set.of("medianFps","p95FrameTimeMs","tps","p95Mspt","maxGcPauseMs","usedMiB","criticalFindings");
        for(TaskResult result:DiagnosticRunService.latestResults()) result.outputs().forEach((key,value)->{if(allowed.contains(key))try{double d=Double.parseDouble(value);if(Double.isFinite(d))metrics.put(key,d);}catch(NumberFormatException ignored){}});
        return new ExperimentSnapshot(Instant.now(), instance.fingerprint(), context, metrics);
    }
    private static String duration(List<TaskResult> results) { return results.stream().flatMap(result -> result.outputs().entrySet().stream()).filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).contains("duration")).map(Map.Entry::getValue).findFirst().orElse("unavailable"); }
    private static String safe(Exception exception){return exception.getMessage()==null?exception.getClass().getSimpleName():exception.getMessage();}
    private static int fail(CommandSourceStack source,String message){source.sendFailure(Component.literal(message));return 0;}
    private static void send(CommandSourceStack source,String message){source.sendSuccess(()->Component.literal(message),false);}
}

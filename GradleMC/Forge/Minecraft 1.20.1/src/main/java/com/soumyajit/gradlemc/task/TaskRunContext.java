package com.soumyajit.gradlemc.task;

import net.minecraft.server.MinecraftServer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TaskRunContext {
    private final MinecraftServer server; private final boolean rerun; private volatile boolean cancelled;
    private final Map<String, TaskOutcome> outcomes = new LinkedHashMap<>();
    private final ExecutionBudget budget;
    private final String requestedId;
    private long filesInspected, bytesRead, samplesCollected, recordsProduced;
    public TaskRunContext(MinecraftServer server, boolean rerun) { this(server, rerun, ExecutionBudget.defaults(), ""); }
    public TaskRunContext(MinecraftServer server, boolean rerun, ExecutionBudget budget) { this(server, rerun, budget, ""); }
    public TaskRunContext(MinecraftServer server, boolean rerun, ExecutionBudget budget, String requestedId) { this.server = server; this.rerun = rerun; this.budget = budget; this.requestedId = requestedId == null ? "" : requestedId; }
    public MinecraftServer server() { return server; } public boolean rerun() { return rerun; }
    public boolean cancelled() { return cancelled; } public void cancel() { cancelled = true; }
    void record(String id, TaskOutcome outcome) { outcomes.put(id, outcome); }
    public Map<String, TaskOutcome> outcomes() { return Collections.unmodifiableMap(outcomes); }
    public ExecutionBudget budget() { return budget; }
    public String requestedId() { return requestedId; }
    public synchronized void accountFiles(long count) { filesInspected = add(filesInspected, count); }
    public synchronized void accountBytes(long count) { bytesRead = add(bytesRead, count); }
    public synchronized void accountSamples(long count) { samplesCollected = add(samplesCollected, count); }
    public synchronized void accountRecords(long count) { recordsProduced = add(recordsProduced, count); }
    public synchronized Counters counters() { return new Counters(filesInspected, bytesRead, samplesCollected, recordsProduced); }
    public synchronized String budgetExceeded() {
        if (filesInspected > budget.maxFilesInspected()) return "maximum files inspected";
        if (bytesRead > budget.maxBytesRead()) return "maximum bytes read";
        if (samplesCollected > budget.maxSamples()) return "maximum samples retained";
        if (recordsProduced > budget.maxRecords()) return "maximum records produced";
        return "";
    }
    private static long add(long current, long count) {
        if (count < 0) throw new IllegalArgumentException("Accounting values cannot be negative");
        return current > Long.MAX_VALUE - count ? Long.MAX_VALUE : current + count;
    }
    public record Counters(long filesInspected, long bytesRead, long samplesCollected, long recordsProduced) { }
}

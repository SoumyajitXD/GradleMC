package com.soumyajit.gradlemc.performance;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/** Command presentation for GradleMC-owned overhead policy; no game settings are changed. */
public final class PerformanceCommandService {
    private PerformanceCommandService() { }
    public static int summary(CommandSourceStack source) {
        PerformanceGuard.Snapshot guard = PerformanceService.guard().snapshot();
        source.sendSuccess(() -> Component.literal("GradleMC performance: mode=" + PerformanceService.mode()
                + ", guard=" + guard.state() + ", optional work deferred=" + guard.optionalWorkDeferred()
                + ", queue=" + PerformanceService.overhead().snapshot().queueDepth()), false);
        return Command.SINGLE_SUCCESS;
    }
    public static int guard(CommandSourceStack source) {
        PerformanceGuard.Snapshot value = PerformanceService.guard().snapshot();
        source.sendSuccess(() -> Component.literal("Performance Guard: " + value.state() + "; " + value.reason()
                + " Sustained pressure=" + value.sustainedPressureSamples() + ", recovery=" + value.recoverySamples()
                + ". Deferred GradleMC work=" + value.deferredWork() + "."), false);
        return Command.SINGLE_SUCCESS;
    }
    public static int explain(CommandSourceStack source) {
        PerformancePolicy policy = PerformanceService.policy();
        PerformanceGuard.Snapshot guard = PerformanceService.guard().snapshot();
        source.sendSuccess(() -> Component.literal("Performance explain: state=" + guard.state()
                + ", fresh evidence=" + guard.evidence().pressureSummary() + ", sustained="
                + guard.sustainedPressureSamples() + ", recovery required=" + (6 - guard.recoverySamples())
                + ", deferred=" + guard.deferredWork() + ", allowed=" + guard.allowedWork()
                + ". Minecraft and third-party settings are untouched. " + policy.overheadWarning()), false);
        return Command.SINGLE_SUCCESS;
    }
    public static int overhead(CommandSourceStack source) {
        GradleMcOverheadMonitor.Snapshot value = PerformanceService.overhead().snapshot();
        source.sendSuccess(() -> Component.literal("GradleMC overhead: workers=" + value.activeWorkers() + ", queue=" + value.queueDepth()
                + ", rejected=" + value.rejectionCount() + ", coalesced=" + value.coalescingOrDeferralCount()
                + ", deferred=" + value.deferredWorkCount() + ", subscriptions=" + value.activeMeasurementSubscriptions()
                + ". Timings are bounded GradleMC wall-time samples, not total Minecraft CPU usage."), false);
        for (GradleMcOverheadMonitor.CategoryStats stat : value.categories()) {
            if (stat.samples() > 0) source.sendSuccess(() -> Component.literal(stat.category() + ": n=" + stat.samples()
                    + ", median=" + stat.medianMillis() + "ms, p95=" + (Double.isFinite(stat.p95Millis()) ? stat.p95Millis() + "ms" : "warming up")
                    + ", max=" + stat.windowMaxMillis() + "ms"), false);
        }
        return Command.SINGLE_SUCCESS;
    }
    public static int mode(CommandSourceStack source, PerformanceMode mode) {
        PerformanceService.setMode(mode);
        source.sendSuccess(() -> Component.literal("GradleMC performance mode set to " + PerformanceService.mode()
                + ". Detailed means diagnostic detail, not game-performance changes."), false);
        return Command.SINGLE_SUCCESS;
    }
    public static int selftest(CommandSourceStack source) {
        PerformanceGuard guard = new PerformanceGuard();
        long now = 20_000_000_000L;
        for (int i = 0; i < 3; i++) guard.observe(now += 1_000_000_000L, 120, 0, 0, 0, false);
        boolean triggered = guard.snapshot().state() == PerformanceGuard.State.CONSTRAINED;
        for (int i = 0; i < 6; i++) guard.observe(now += 2_000_000_000L, 10, 10, 0, 0, false);
        boolean recovered = guard.snapshot().state() != PerformanceGuard.State.CONSTRAINED;
        source.sendSuccess(() -> Component.literal("Performance Guard deterministic self-test: " + (triggered && recovered ? "passed" : "failed") + "."), false);
        return triggered && recovered ? Command.SINGLE_SUCCESS : 0;
    }
}

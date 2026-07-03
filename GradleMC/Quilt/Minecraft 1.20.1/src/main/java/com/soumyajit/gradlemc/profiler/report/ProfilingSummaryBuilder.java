package com.soumyajit.gradlemc.profiler.report;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.profiler.ProfilerSession;
import com.soumyajit.gradlemc.profiler.sampling.StackTraceAggregator;
import org.quiltmc.loader.api.QuiltLoader;
import net.minecraft.SharedConstants;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProfilingSummaryBuilder {
    public ProfilingSummary build(ProfilerSession session) {
        List<ModAttribution> attributions = attributions(session.stackAggregator().packageCounts());
        return new ProfilingSummary(
                GradleMCVersion.lookup(),
                SharedConstants.getCurrentVersion().getName(),
                loaderVersion(),
                System.getProperty("java.version", "unknown"),
                System.getProperty("java.vendor", "unknown"),
                System.getProperty("os.name", "unknown") + " " + System.getProperty("os.arch", ""),
                QuiltLoader.getAllMods().size(),
                session.startedAt(),
                session.endedAt(),
                Duration.between(session.startedAt(), session.endedAt()),
                session.tickRecorder().summary(),
                session.tickRecorder().slowest(10),
                session.slowTickSnapshots(),
                session.stackAggregator().sampleCount(),
                session.stackAggregator().topThreads(10),
                session.stackAggregator().topFrames(20),
                session.stackAggregator().topLeaves(20),
                session.stackAggregator().topPackages(20),
                attributions,
                session.heapStartMiB(),
                session.heapEndMiB(),
                session.maxHeapMiB(),
                session.gcCountDelta(),
                session.gcTimeDeltaMillis(),
                interpretation(session, attributions)
        );
    }

    private static List<ModAttribution> attributions(Map<String, Long> packageCounts) {
        List<ModAttribution> result = new ArrayList<>();
        packageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(20)
                .forEach(entry -> result.add(classify(entry.getKey(), entry.getValue())));
        return result;
    }

    private static ModAttribution classify(String packageName, long samples) {
        String lower = packageName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("net.minecraft")) {
            return new ModAttribution("Minecraft internals", "HIGH", samples, packageName);
        }
        if (lower.startsWith("net.fabricmc") || lower.startsWith("fabric.")) {
            return new ModAttribution("Quilt/Fabric API internals", "HIGH", samples, packageName);
        }
        if (lower.startsWith("java.") || lower.startsWith("jdk.") || lower.startsWith("sun.")) {
            return new ModAttribution("JVM/Java runtime", "HIGH", samples, packageName);
        }
        if (lower.startsWith("com.soumyajit.gradlemc")) {
            return new ModAttribution("GradleMC", "HIGH", samples, packageName);
        }
        return QuiltLoader.getAllMods().stream()
                .filter(mod -> lower.contains(mod.metadata().id().toLowerCase(Locale.ROOT)))
                .findFirst()
                .map(mod -> new ModAttribution(mod.metadata().id() + " (" + mod.metadata().name() + ")", "LOW", samples,
                        "Weak package/mod-id name similarity: " + packageName))
                .orElseGet(() -> new ModAttribution("Unknown package", "UNKNOWN", samples, packageName));
    }

    private static String interpretation(ProfilerSession session, List<ModAttribution> attributions) {
        double max = session.tickRecorder().summary().maxMspt();
        if (max >= 250.0D && session.gcTimeDeltaMillis() > 0L) {
            return "Suspicious: slow ticks occurred while GC activity was detected. This suggests memory pressure, not proven allocation attribution.";
        }
        if (max >= 100.0D) {
            return "Suspicious: slow ticks were recorded. Check the slowest tick table and top sampled Java frames.";
        }
        if (!attributions.isEmpty() && attributions.get(0).confidence().equals("LOW")) {
            return "Possible contributor packages were seen in samples, but confidence is low. Treat them as leads, not blame.";
        }
        return "No severe spike was proven by this bounded session. Longer or more targeted profiling may be needed.";
    }

    private static final class GradleMCVersion {
        private static String lookup() {
            return QuiltLoader
                    .getModContainer(GradleMC.MOD_ID)
                    .map(container -> container.metadata().version().raw())
                    .orElse("unknown");
        }
    }

    private static String loaderVersion() {
        return QuiltLoader
                .getModContainer("quilt_loader")
                .map(container -> container.metadata().version().raw())
                .orElse("unknown");
    }
}

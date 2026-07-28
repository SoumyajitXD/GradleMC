package com.soumyajit.gradlemc.profiler;

import com.soumyajit.gradlemc.capability.CapabilitySnapshot;
import com.soumyajit.gradlemc.capability.RuntimeCapability;

import java.util.ArrayList;
import java.util.List;

/** Pure GUI/command gating policy; execution still enforces permission on the server. */
public final class ProfilerAvailability {
    public static final int MAX_CUSTOM_FILTER_LENGTH = 64;
    public static final int MIN_ALL_THREADS_INTERVAL_MILLIS = 20;

    public enum Reason {
        AVAILABLE,
        NO_LOGICAL_SERVER,
        STALE_SERVER_CAPABILITY,
        SERVER_PERMISSION_REQUIRED,
        REMOTE_THREAD_NOT_LOCAL,
        EMPTY_CUSTOM_FILTER,
        CUSTOM_FILTER_TOO_LONG,
        CUSTOM_FILTER_TOO_BROAD,
        ALL_THREADS_INTERVAL_TOO_LOW,
        COMBINED_REQUIRES_ALL_COLLECTORS
    }

    public record Plan(boolean runnable, boolean full, Reason reason, String explanation,
                       List<String> collectors, List<String> owners, String warning) {
        public Plan {
            reason = reason == null ? Reason.NO_LOGICAL_SERVER : reason;
            explanation = explanation == null ? "" : explanation;
            collectors = List.copyOf(collectors == null ? List.of() : collectors);
            owners = List.copyOf(owners == null ? List.of() : owners);
            warning = warning == null ? "" : warning;
        }
    }

    private ProfilerAvailability() { }

    public static Plan decide(CapabilitySnapshot capabilities, ProfilerSessionConfig requested) {
        String requestedPattern = requested == null ? ProfilerSessionConfig.defaults().threadPattern() : requested.threadPattern();
        ProfilerSessionConfig config = requested == null ? ProfilerSessionConfig.defaults() : requested.sanitized();
        if (capabilities == null) return unavailable(Reason.NO_LOGICAL_SERVER);
        ProfilerThreadScope scope = ProfilerThreadScope.fromPattern(requestedPattern);
        Reason filterFailure = validateFilter(requestedPattern, scope);
        if (filterFailure != null) return unavailable(filterFailure);
        if (scope == ProfilerThreadScope.ALL && config.intervalMillis() < MIN_ALL_THREADS_INTERVAL_MILLIS) {
            return unavailable(Reason.ALL_THREADS_INTERVAL_TOO_LOW);
        }
        boolean serverReady = capabilities.hasFreshServerEvidence()
                && capabilities.available(RuntimeCapability.LOGICAL_SERVER_STATUS);
        boolean serverProfiling = serverReady && capabilities.available(RuntimeCapability.SERVER_THREAD_PROFILING);
        boolean tickProfiling = serverReady && capabilities.available(RuntimeCapability.TICK_PROFILING);
        if (capabilities.stale() && (config.mode().recordsTicks() || scope == ProfilerThreadScope.SERVER)) {
            return unavailable(Reason.STALE_SERVER_CAPABILITY);
        }
        List<String> collectors = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        if (config.mode().recordsTicks()) {
            if (!tickProfiling) return unavailable(Reason.NO_LOGICAL_SERVER);
            collectors.add("tick"); owners.add("logical-server");
        }
        if (config.mode().samplesCpu()) {
            if (scope == ProfilerThreadScope.RENDER) {
                if (!capabilities.available(RuntimeCapability.RENDER_THREAD_CPU_LITE)) return unavailable(Reason.NO_LOGICAL_SERVER);
                collectors.add("cpu-lite-render"); owners.add("local-client");
            } else {
                if (!serverProfiling) return unavailable(Reason.NO_LOGICAL_SERVER);
                if (!capabilities.serverActionAllowed(RuntimeCapability.SERVER_THREAD_PROFILING)) return unavailable(Reason.SERVER_PERMISSION_REQUIRED);
                collectors.add(scope == ProfilerThreadScope.ALL ? "cpu-lite-all-bounded" : "cpu-lite-server");
                owners.add("logical-server");
            }
        }
        if (config.mode().recordsMemory()) {
            if (!capabilities.available(RuntimeCapability.LOCAL_JVM_MEMORY)) return unavailable(Reason.NO_LOGICAL_SERVER);
            collectors.add("memory-lite-client-process"); owners.add("local-client");
        }
        if (config.mode() == ProfilerMode.COMBINED && collectors.size() != 3) {
            return unavailable(Reason.COMBINED_REQUIRES_ALL_COLLECTORS);
        }
        String warning = scope == ProfilerThreadScope.ALL ? "Higher overhead: sampling is bounded to a small thread and stack limit." : "";
        return new Plan(true, true, Reason.AVAILABLE, "Available", collectors, owners, warning);
    }

    private static Reason validateFilter(String requestedPattern, ProfilerThreadScope scope) {
        if (scope != ProfilerThreadScope.CUSTOM) return null;
        String filter = requestedPattern == null ? "" : requestedPattern.trim();
        if (filter.isEmpty()) return Reason.EMPTY_CUSTOM_FILTER;
        if (filter.length() > MAX_CUSTOM_FILTER_LENGTH) return Reason.CUSTOM_FILTER_TOO_LONG;
        if (filter.equals("*") || filter.equalsIgnoreCase("all") || filter.length() < 3) return Reason.CUSTOM_FILTER_TOO_BROAD;
        return null;
    }

    private static Plan unavailable(Reason reason) {
        return new Plan(false, false, reason, explanation(reason), List.of(), List.of(), "");
    }

    private static String explanation(Reason reason) {
        return switch (reason) {
            case NO_LOGICAL_SERVER -> "A ready logical server capability is required.";
            case STALE_SERVER_CAPABILITY -> "Server capability information is stale.";
            case SERVER_PERMISSION_REQUIRED -> "The server has not authorised administrative profiling.";
            case EMPTY_CUSTOM_FILTER -> "Enter a bounded custom thread filter.";
            case CUSTOM_FILTER_TOO_LONG -> "Custom thread filters are limited to 64 characters.";
            case CUSTOM_FILTER_TOO_BROAD -> "Custom thread filters must be specific.";
            case ALL_THREADS_INTERVAL_TOO_LOW -> "All-thread sampling requires an interval of at least 20 ms.";
            case COMBINED_REQUIRES_ALL_COLLECTORS -> "Combined requires every collector; it does not silently degrade.";
            default -> "Unavailable.";
        };
    }
}

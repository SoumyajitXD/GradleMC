package com.soumyajit.gradlemc.network;

/**
 * Immutable client-side view of the currently connected GradleMC server.
 * This class deliberately contains no client or server runtime references so
 * it can be used by GUI view models without compromising dedicated servers.
 */
public record ServerCapabilityState(
        Availability availability,
        int protocolVersion,
        boolean serverDiagnosticsAllowed,
        boolean administrativeDiagnosticsAllowed,
        String serverVersion,
        String reason,
        long connectionGeneration
) {
    public enum Availability {
        LOCAL_ONLY,
        NEGOTIATING,
        AVAILABLE,
        UNSUPPORTED,
        INCOMPATIBLE,
        DISCONNECTED
    }

    private static final int MAX_REASON_LENGTH = 160;
    private static final int MAX_VERSION_LENGTH = 32;

    public ServerCapabilityState {
        availability = availability == null ? Availability.LOCAL_ONLY : availability;
        protocolVersion = Math.max(0, protocolVersion);
        serverVersion = bounded(serverVersion, MAX_VERSION_LENGTH);
        reason = bounded(reason, MAX_REASON_LENGTH);
        connectionGeneration = Math.max(0L, connectionGeneration);
    }

    public static ServerCapabilityState localOnly(long generation, String reason) {
        return new ServerCapabilityState(Availability.LOCAL_ONLY, 0, false, false, "", reason, generation);
    }

    public static ServerCapabilityState negotiating(long generation) {
        return new ServerCapabilityState(Availability.NEGOTIATING, 0, false, false, "", "Checking GradleMC server support…", generation);
    }

    public boolean supportsServerOperations() {
        return availability == Availability.AVAILABLE && serverDiagnosticsAllowed;
    }

    private static String bounded(String value, int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}

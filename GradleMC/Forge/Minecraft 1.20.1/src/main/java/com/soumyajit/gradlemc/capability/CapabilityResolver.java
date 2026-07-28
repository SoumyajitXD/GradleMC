package com.soumyajit.gradlemc.capability;

import java.util.EnumSet;

/** Pure capability policy.  Forge/client adapters provide evidence; this class owns no game objects. */
public final class CapabilityResolver {
    private CapabilityResolver() { }

    public static CapabilitySnapshot resolve(CapabilityInput input) {
        if (input == null) input = new CapabilityInput(false, false, false, false,
                CapabilityInput.ConnectionState.DISCONNECTED, false, false, false, false, java.util.Set.of());
        RuntimeLocation location = location(input);
        NetworkCompatibility network = network(input);
        boolean fresh = network == NetworkCompatibility.CAPABILITY_SNAPSHOT_FRESH;
        EnumSet<RuntimeCapability> available = EnumSet.noneOf(RuntimeCapability.class);
        EnumSet<RuntimeCapability> actions = EnumSet.noneOf(RuntimeCapability.class);
        if (input.physicalClient()) {
            available.addAll(EnumSet.of(RuntimeCapability.FPS_SAMPLING, RuntimeCapability.ROLLING_FPS_STATISTICS,
                    RuntimeCapability.RENDER_THREAD_CPU_LITE, RuntimeCapability.LOCAL_JVM_MEMORY,
                    RuntimeCapability.LOCAL_REPORT_ACCESS, RuntimeCapability.LOCAL_OVERLAY_SETTINGS));
        }
        if (fresh || location == RuntimeLocation.INTEGRATED_SERVER_READY) {
            for (ServerOperation operation : input.serverOperations()) available.add(operation.capability());
        }
        PermissionState permission = fresh || location == RuntimeLocation.INTEGRATED_SERVER_READY
                ? (input.administrativePermission() ? PermissionState.ADMINISTRATIVE_ALLOWED : PermissionState.READ_ONLY)
                : PermissionState.UNKNOWN;
        if (fresh && permission == PermissionState.ADMINISTRATIVE_ALLOWED) {
            for (ServerOperation operation : input.serverOperations()) actions.add(operation.capability());
        }
        OutputLocality locality = switch (location) {
            case INTEGRATED_SERVER_READY, INTEGRATED_SERVER_STARTING -> OutputLocality.INTEGRATED_PROCESS_OUTPUT;
            case REMOTE_MULTIPLAYER -> OutputLocality.REMOTE_SERVER_ONLY;
            case LOCAL_CLIENT_NO_LOGICAL_SERVER -> OutputLocality.LOCAL_CLIENT_OUTPUT;
            case NO_ACTIVE_WORLD -> input.physicalClient() ? OutputLocality.LOCAL_CLIENT_OUTPUT : OutputLocality.NO_LOCAL_READABLE_PATH;
        };
        return new CapabilitySnapshot(location, network, permission, locality, available, actions,
                network == NetworkCompatibility.CAPABILITY_SNAPSHOT_STALE);
    }

    private static RuntimeLocation location(CapabilityInput input) {
        if (!input.activeWorld()) return RuntimeLocation.NO_ACTIVE_WORLD;
        if (input.integratedServerPresent()) return input.logicalServerReady()
                ? RuntimeLocation.INTEGRATED_SERVER_READY : RuntimeLocation.INTEGRATED_SERVER_STARTING;
        return input.connectionState() == CapabilityInput.ConnectionState.DISCONNECTED
                ? RuntimeLocation.LOCAL_CLIENT_NO_LOGICAL_SERVER : RuntimeLocation.REMOTE_MULTIPLAYER;
    }

    private static NetworkCompatibility network(CapabilityInput input) {
        if (input.snapshotErrored()) return NetworkCompatibility.CAPABILITY_SNAPSHOT_ERROR;
        if (input.acceptedServerSnapshot()) return input.snapshotStale()
                ? NetworkCompatibility.CAPABILITY_SNAPSHOT_STALE : NetworkCompatibility.CAPABILITY_SNAPSHOT_FRESH;
        return switch (input.connectionState()) {
            case DISCONNECTED -> NetworkCompatibility.DISCONNECTED;
            case CONNECTING -> NetworkCompatibility.CONNECTION_STARTING;
            case CHANNEL_AVAILABLE -> NetworkCompatibility.CAPABILITY_SNAPSHOT_PENDING;
            case REMOTE_WITHOUT_GRADLEMC -> NetworkCompatibility.REMOTE_SERVER_WITHOUT_GRADLEMC;
            case INCOMPATIBLE -> NetworkCompatibility.INCOMPATIBLE_PROTOCOL;
        };
    }
}

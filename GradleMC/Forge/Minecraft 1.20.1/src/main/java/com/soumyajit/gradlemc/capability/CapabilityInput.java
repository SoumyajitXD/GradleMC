package com.soumyajit.gradlemc.capability;

import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable, Minecraft-class-free evidence supplied by side-specific adapters.  A remote client
 * cannot create administrative permission: that field is only meaningful after an accepted
 * server snapshot.
 */
public record CapabilityInput(
        boolean physicalClient,
        boolean activeWorld,
        boolean integratedServerPresent,
        boolean logicalServerReady,
        ConnectionState connectionState,
        boolean acceptedServerSnapshot,
        boolean snapshotStale,
        boolean snapshotErrored,
        boolean administrativePermission,
        Set<ServerOperation> serverOperations
) {
    public enum ConnectionState { DISCONNECTED, CONNECTING, CHANNEL_AVAILABLE, REMOTE_WITHOUT_GRADLEMC, INCOMPATIBLE }

    public CapabilityInput {
        connectionState = connectionState == null ? ConnectionState.DISCONNECTED : connectionState;
        serverOperations = serverOperations == null || serverOperations.isEmpty()
                ? Set.of() : Set.copyOf(EnumSet.copyOf(serverOperations));
    }
}

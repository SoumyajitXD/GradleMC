package com.soumyajit.gradlemc.capability;

import java.util.EnumSet;
import java.util.Set;

/** One coherent immutable replacement for client-visible runtime capability state. */
public record CapabilitySnapshot(
        RuntimeLocation location,
        NetworkCompatibility network,
        PermissionState permission,
        OutputLocality outputLocality,
        Set<RuntimeCapability> available,
        Set<RuntimeCapability> serverActions,
        boolean stale
) {
    public CapabilitySnapshot {
        location = location == null ? RuntimeLocation.NO_ACTIVE_WORLD : location;
        network = network == null ? NetworkCompatibility.DISCONNECTED : network;
        permission = permission == null ? PermissionState.UNKNOWN : permission;
        outputLocality = outputLocality == null ? OutputLocality.NO_LOCAL_READABLE_PATH : outputLocality;
        available = immutable(available);
        serverActions = immutable(serverActions);
    }

    private static Set<RuntimeCapability> immutable(Set<RuntimeCapability> value) {
        return value == null || value.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(value));
    }

    public boolean available(RuntimeCapability capability) {
        return available.contains(capability);
    }

    /** Only fresh, server-authorised operations may enable an action. */
    public boolean serverActionAllowed(RuntimeCapability capability) {
        return !stale && permission == PermissionState.ADMINISTRATIVE_ALLOWED && serverActions.contains(capability);
    }

    public boolean hasFreshServerEvidence() {
        return network == NetworkCompatibility.CAPABILITY_SNAPSHOT_FRESH && !stale;
    }
}

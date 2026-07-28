package com.soumyajit.gradlemc.capability;

/** Transport state is intentionally separate from permission and server readiness. */
public enum NetworkCompatibility {
    DISCONNECTED,
    CONNECTION_STARTING,
    CHANNEL_AVAILABLE,
    REMOTE_SERVER_WITHOUT_GRADLEMC,
    INCOMPATIBLE_PROTOCOL,
    CAPABILITY_SNAPSHOT_PENDING,
    CAPABILITY_SNAPSHOT_FRESH,
    CAPABILITY_SNAPSHOT_STALE,
    CAPABILITY_SNAPSHOT_ERROR
}

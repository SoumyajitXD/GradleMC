package com.soumyajit.gradlemc.capability;

/** Where the client is relative to a logical Minecraft server. */
public enum RuntimeLocation {
    NO_ACTIVE_WORLD,
    LOCAL_CLIENT_NO_LOGICAL_SERVER,
    INTEGRATED_SERVER_STARTING,
    INTEGRATED_SERVER_READY,
    REMOTE_MULTIPLAYER
}

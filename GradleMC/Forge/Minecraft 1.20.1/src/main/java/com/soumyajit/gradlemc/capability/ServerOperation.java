package com.soumyajit.gradlemc.capability;

/** Operations asserted by the server in an accepted dashboard snapshot. */
public enum ServerOperation {
    STATUS(RuntimeCapability.LOGICAL_SERVER_STATUS),
    TPS_MSPT(RuntimeCapability.TPS_MSPT_OBSERVATION),
    SERVER_PROFILER(RuntimeCapability.SERVER_THREAD_PROFILING),
    TICK_PROFILER(RuntimeCapability.TICK_PROFILING),
    SERVER_MEMORY(RuntimeCapability.SERVER_JVM_EVIDENCE),
    ENTITIES(RuntimeCapability.ENTITY_DIAGNOSTICS),
    BLOCK_ENTITIES(RuntimeCapability.BLOCK_ENTITY_DIAGNOSTICS),
    WORLDGEN(RuntimeCapability.WORLDGEN_OBSERVATION),
    ADMIN_ACTIONS(RuntimeCapability.ADMINISTRATIVE_ACTIONS);

    private final RuntimeCapability capability;

    ServerOperation(RuntimeCapability capability) {
        this.capability = capability;
    }

    public RuntimeCapability capability() {
        return capability;
    }
}

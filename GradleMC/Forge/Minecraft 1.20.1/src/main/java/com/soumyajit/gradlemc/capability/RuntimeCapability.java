package com.soumyajit.gradlemc.capability;

/** A bounded set of facts which the GUI may present.  It is never an authority for commands. */
public enum RuntimeCapability {
    FPS_SAMPLING,
    ROLLING_FPS_STATISTICS,
    RENDER_THREAD_CPU_LITE,
    LOCAL_JVM_MEMORY,
    LOCAL_REPORT_ACCESS,
    LOCAL_OVERLAY_SETTINGS,
    LOGICAL_SERVER_STATUS,
    TPS_MSPT_OBSERVATION,
    SERVER_THREAD_PROFILING,
    TICK_PROFILING,
    SERVER_JVM_EVIDENCE,
    ENTITY_DIAGNOSTICS,
    BLOCK_ENTITY_DIAGNOSTICS,
    WORLDGEN_OBSERVATION,
    ADMINISTRATIVE_ACTIONS
}

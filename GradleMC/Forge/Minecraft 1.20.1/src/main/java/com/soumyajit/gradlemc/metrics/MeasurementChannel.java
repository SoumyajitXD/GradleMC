package com.soumyajit.gradlemc.metrics;

/** Bounded set of live signals owned by the GradleMC measurement hub. */
public enum MeasurementChannel {
    FRAME_TIMING,
    JVM_MEMORY,
    SERVER_TICK
}

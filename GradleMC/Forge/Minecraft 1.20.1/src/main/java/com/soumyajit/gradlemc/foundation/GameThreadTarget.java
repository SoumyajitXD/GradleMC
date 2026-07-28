package com.soumyajit.gradlemc.foundation;

/** Owning execution context for a Foundation capture. */
public enum GameThreadTarget {
    SERVER_MAIN_THREAD_CAPTURE,
    CLIENT_MAIN_THREAD_CAPTURE,
    WORKER_SAFE,
    CALLER_SAFE
}

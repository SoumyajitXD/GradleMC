package com.soumyajit.gradlemc.task;

final class DiagnosticCancelledException extends RuntimeException {
    DiagnosticCancelledException(String message) { super(message); }
}

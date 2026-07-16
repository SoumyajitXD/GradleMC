package com.soumyajit.gradlemc.instance;

public record ConfigDescriptor(String fileName, String extension, long sizeBytes, long lastModifiedMillis,
                               String parseStatus, String ownerHint, String warning) {
    public ConfigDescriptor {
        fileName = fileName == null ? "" : fileName;
        extension = extension == null ? "" : extension;
        parseStatus = parseStatus == null ? "not-parsed" : parseStatus;
        ownerHint = ownerHint == null ? "" : ownerHint;
        warning = warning == null ? "" : warning;
    }
}

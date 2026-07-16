package com.soumyajit.gradlemc.instance;

import java.util.List;

/** Metadata-only pack record; never contains an absolute path or pack contents. */
public record PackDescriptor(String id, String fileName, PackKind kind, boolean archive, long sizeBytes,
                             long lastModifiedMillis, int packFormat, String parseStatus,
                             List<String> namespaces, List<String> warnings) {
    public enum PackKind { RESOURCE, SHADER, DATA }
    public PackDescriptor {
        id = id == null ? "" : id;
        fileName = fileName == null ? "" : fileName;
        parseStatus = parseStatus == null ? "unavailable" : parseStatus;
        namespaces = namespaces == null ? List.of() : List.copyOf(namespaces);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

package com.soumyajit.gradlemc.modaudit;

/** A declared Forge dependency, kept as data rather than display text. */
public record ModDependencyDescriptor(String modId, String versionRange, boolean mandatory,
                                      String side, String ordering) {
    public ModDependencyDescriptor {
        modId = ModDescriptor.normalize(modId);
        versionRange = versionRange == null ? "" : versionRange;
        side = side == null ? "BOTH" : side;
        ordering = ordering == null ? "NONE" : ordering;
    }
}

package com.soumyajit.gradlemc.instance;

import com.soumyajit.gradlemc.modaudit.InstalledModSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Canonical instance model. Dynamic Minecraft access belongs in providers, not consumers. */
public record MinecraftInstanceSnapshot(Instant collectedAt, InstanceComponent<Map<String, String>> environment,
                                        InstanceComponent<Map<String, String>> runtime,
                                        InstanceComponent<InstalledModSnapshot> mods,
                                        InstanceComponent<List<PackDescriptor>> resourcePacks,
                                        InstanceComponent<List<PackDescriptor>> shaderPacks,
                                        InstanceComponent<List<PackDescriptor>> dataPacks,
                                        InstanceComponent<List<ConfigDescriptor>> configs,
                                        InstanceComponent<Map<String, String>> world,
                                        InstanceComponent<Map<String, String>> providers,
                                        String fingerprint) {
    public MinecraftInstanceSnapshot {
        collectedAt = Objects.requireNonNull(collectedAt, "collectedAt");
        fingerprint = fingerprint == null ? "" : fingerprint;
    }
}

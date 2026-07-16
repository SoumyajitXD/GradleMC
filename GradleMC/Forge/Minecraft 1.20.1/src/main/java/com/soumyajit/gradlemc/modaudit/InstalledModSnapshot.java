package com.soumyajit.gradlemc.modaudit;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Read-only snapshot; all collections are normalized and sorted for stable reports. */
public record InstalledModSnapshot(Instant capturedAt, List<ModDescriptor> mods, boolean available,
                                   String unavailableReason) {
    public InstalledModSnapshot {
        capturedAt = capturedAt == null ? Instant.EPOCH : capturedAt;
        mods = mods == null ? List.of() : mods.stream().sorted(Comparator.comparing(ModDescriptor::modId)).toList();
        unavailableReason = unavailableReason == null ? "" : unavailableReason;
    }
    public static InstalledModSnapshot unavailable(String reason) { return new InstalledModSnapshot(Instant.now(), List.of(), false, reason); }
    public Optional<ModDescriptor> find(String id) { String key = ModDescriptor.normalize(id); return mods.stream().filter(mod -> mod.modId().equals(key)).findFirst(); }
    public Map<String, ModDescriptor> byId() { Map<String, ModDescriptor> map = new TreeMap<>(); mods.forEach(mod -> map.put(mod.modId(), mod)); return Map.copyOf(map); }
    public int owningFileCount() { return (int) mods.stream().map(ModDescriptor::jarFileName).filter(name -> !name.isBlank()).distinct().count(); }
}

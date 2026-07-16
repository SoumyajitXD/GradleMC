package com.soumyajit.gradlemc.lock;

import java.util.List;
import java.util.Map;

public record InstanceLockSnapshot(int schemaVersion, String minecraft, String forge, String javaMajor,
                                   String javaRuntime, String gradleMc, List<LockedMod> mods,
                                   List<String> resourcePacks, List<String> shaderPacks, List<String> dataPacks,
                                   String configFingerprint, Map<String, String> providerCapabilities,
                                   List<String> unavailable) {
    public InstanceLockSnapshot {
        mods=List.copyOf(mods);resourcePacks=List.copyOf(resourcePacks);shaderPacks=List.copyOf(shaderPacks);dataPacks=List.copyOf(dataPacks);
        providerCapabilities=Map.copyOf(providerCapabilities);unavailable=List.copyOf(unavailable);
    }
    public record LockedMod(String id,String version,String jarFile,List<String> dependencies){public LockedMod{dependencies=List.copyOf(dependencies);}}
}

package com.soumyajit.gradlemc.modaudit;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Isolates supported Forge metadata access from commands, reports, and client UI. */
public final class ForgeInstalledModSnapshotProvider {
    private static volatile InstalledModSnapshot cached;
    private ForgeInstalledModSnapshotProvider() { }
    public static InstalledModSnapshot current() { InstalledModSnapshot value = cached; return value == null ? refresh() : value; }
    public static synchronized InstalledModSnapshot refresh() {
        try {
            List<ModDescriptor> descriptors = new ArrayList<>();
            for (IModInfo mod : ModList.get().getMods()) descriptors.add(describe(mod));
            cached = new InstalledModSnapshot(Instant.now(), descriptors, true, "");
        } catch (RuntimeException exception) {
            cached = InstalledModSnapshot.unavailable(exception.getClass().getSimpleName());
        }
        return cached;
    }
    private static ModDescriptor describe(IModInfo mod) {
        List<ModDependencyDescriptor> dependencies = mod.getDependencies().stream().map(dependency ->
                new ModDependencyDescriptor(dependency.getModId(), dependency.getVersionRange().toString(), dependency.isMandatory(),
                        dependency.getSide().name(), dependency.getOrdering().name())).toList();
        List<String> observations = new ArrayList<>();
        String display = safe(mod.getDisplayName());
        String version = safe(mod.getVersion().toString());
        if (display.isBlank() || display.equalsIgnoreCase(mod.getModId())) observations.add("Display name is missing or falls back to the mod ID.");
        if (version.equals("1")) observations.add("Version is the generic value '1'; this may be intentional but reduces diagnostic precision.");
        if (safe(mod.getDescription()).isBlank()) observations.add("Description is missing.");
        String fileName = ""; int entries = 0;
        try { fileName = mod.getOwningFile().getFile().getFileName(); entries = mod.getOwningFile().getMods().size(); }
        catch (RuntimeException ignored) { observations.add("Owning JAR filename was unavailable from Forge metadata."); }
        return new ModDescriptor(mod.getModId(), mod.getNamespace(), display, version, fileName, entries, dependencies,
                mod.getUpdateURL().isPresent(), mod.getModURL().isPresent(), observations);
    }
    private static String safe(String value) { return value == null ? "" : value; }
}

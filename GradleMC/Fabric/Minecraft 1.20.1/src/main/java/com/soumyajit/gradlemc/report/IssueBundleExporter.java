package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.util.AtomicFiles;
import com.soumyajit.gradlemc.util.GradleMcLimits;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.RedactionService;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Privacy-first local exporter. It never walks game/profile directories or includes external files. */
public final class IssueBundleExporter {
    private static final String BUNDLE_SCHEMA = "gradlemc-issue-bundle-v2";

    public Path create(MinecraftServer server) throws IOException {
        Path directory = GradleMcPaths.issueBundleDirectory();
        Files.createDirectories(directory);
        if (Files.isSymbolicLink(directory)) throw new IOException("GradleMC issue-bundle directory may not be a symbolic link");
        Path output = ReportFileNames.unique(directory, "gradlemc-issue-bundle-", Instant.now(), ".zip");
        Path temporary = Files.createTempFile(directory, ".gradlemc-issue-", ".tmp");
        RedactionService redactor = new RedactionService(GradleMcPaths.gameDirectory(), homeDirectory());
        List<ManifestItem> manifest = new ArrayList<>();
        try (OutputStream stream = Files.newOutputStream(temporary);
             ZipOutputStream zip = new ZipOutputStream(stream, StandardCharsets.UTF_8)) {
            Set<String> names = new HashSet<>();
            addText(zip, names, manifest, "HOW_TO_REPORT.txt", "instructions", howToReport(), redactor);
            addText(zip, names, manifest, "environment-summary.txt", "generated-environment", environmentSummary(server), redactor);
            addText(zip, names, manifest, "mod-list-summary.txt", "generated-mod-inventory", modListSummary(), redactor);
            addText(zip, names, manifest, "gradlemc-config-summary.txt", "generated-config-summary", configSummary(), redactor);
            addText(zip, names, manifest, "manifest.txt", "bundle-manifest", manifestText(manifest), redactor);
        } catch (IOException failure) {
            Files.deleteIfExists(temporary);
            throw failure;
        }
        try {
            if (Files.size(temporary) > GradleMcLimits.MAX_ISSUE_BUNDLE_BYTES) {
                throw new IOException("Issue bundle exceeds GradleMC's " + GradleMcLimits.MAX_ISSUE_BUNDLE_BYTES + " byte limit");
            }
            AtomicFiles.replace(temporary, output);
            return output;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void addText(ZipOutputStream zip, Set<String> names, List<ManifestItem> manifest, String name,
                         String category, String text, RedactionService redactor) throws IOException {
        RedactionService.Result result = redactor.redact(text);
        addText(zip, names, manifest, name, category, result.text(), redactor, result.redactionCount(), result.truncated());
    }

    private void addText(ZipOutputStream zip, Set<String> names, List<ManifestItem> manifest, String name,
                         String category, String text, RedactionService redactor, int redactions, boolean truncated) throws IOException {
        if (!safeEntryName(name) || !names.add(name) || manifest.size() >= GradleMcLimits.MAX_ISSUE_BUNDLE_INPUTS) throw new IOException("Unsafe or excessive issue-bundle entry");
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > GradleMcLimits.MAX_ISSUE_BUNDLE_ENTRY_BYTES) throw new IOException("Issue-bundle entry is too large: " + name);
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
        manifest.add(new ManifestItem(name, category, bytes.length, redactions, truncated));
    }

    static boolean safeEntryName(String name) {
        return name != null && !name.isBlank() && name.length() <= 128
                && name.matches("[A-Za-z0-9][A-Za-z0-9._/-]*")
                && !name.contains("..") && !name.contains(":") && !name.startsWith("/");
    }

    private String environmentSummary(MinecraftServer server) {
        RuntimeSnapshots.MemorySnapshot memory = RuntimeSnapshots.memory();
        return "Product: " + GradleMC.PRODUCT_NAME + "\nVersion: " + GradleMC.version() + "\nVariant: " + GradleMC.CURRENT_DISPLAY_VARIANT + "\nMinecraft: " + SharedConstants.getCurrentVersion().getName()
                + "\nJava: " + System.getProperty("java.version", "unknown") + "\nLoaded mods: " + FabricLoader.getInstance().getAllMods().size()
                + "\nPlayers online: " + (server == null ? "unavailable" : server.getPlayerCount()) + "\nJVM heap used/committed/max: " + memory.usedMiB() + "/" + memory.totalMiB() + "/" + memory.maxMiB() + " MiB\n";
    }

    private String modListSummary() {
        StringBuilder value = new StringBuilder("Loaded mods: ").append(FabricLoader.getInstance().getAllMods().size()).append("\n\n");
        FabricLoader.getInstance().getAllMods().stream().sorted(Comparator.comparing(mod -> mod.getMetadata().getId())).limit(2_000)
                .forEach(mod -> value.append(mod.getMetadata().getId()).append(" ").append(mod.getMetadata().getVersion().getFriendlyString()).append('\n'));
        return value.toString();
    }

    private String configSummary() {
        return "reportsEnabled: " + GradleMCConfig.REPORTS_ENABLED.get() + "\nissueBundleEnabled: " + GradleMCConfig.ISSUE_BUNDLE_ENABLED.get()
                + "\nlogsIncluded: false (not configurable)\noverlayEnabled: " + GradleMCConfig.OVERLAY_ENABLED.get() + "\n";
    }

    private String manifestText(List<ManifestItem> items) {
        StringBuilder value = new StringBuilder("schema: ").append(BUNDLE_SCHEMA).append("\nversion: ").append(GradleMC.version()).append("\nentries:\n");
        for (ManifestItem item : items) value.append("- ").append(item.name).append(" | ").append(item.category).append(" | ").append(item.bytes).append(" bytes | redactions=").append(item.redactions).append(" | truncated=").append(item.truncated).append('\n');
        value.append("Omitted by policy: all Minecraft logs, crash reports, saves, screenshots, mods, and non-GradleMC files.\n");
        return value.toString();
    }

    private String howToReport() { return "GradleMC Issue Bundle\n\nThis local bundle contains only bounded GradleMC-generated summaries and reports. Review it before sharing; redaction is best-effort. No logs, saves, mods, screenshots, profile files, telemetry, or uploads are included.\n"; }
    private Path homeDirectory() { String home = System.getProperty("user.home"); return home == null || home.isBlank() ? null : Path.of(home); }
    private record ManifestItem(String name, String category, long bytes, int redactions, boolean truncated) { }
}

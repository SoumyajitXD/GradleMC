package com.soumyajit.gradlemc.client.gui;

import com.soumyajit.gradlemc.foundation.GradleMcRuntimeExecutor;
import com.soumyajit.gradlemc.report.LocalReportIndex;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.client.Minecraft;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Client-thread-visible immutable catalog with one bounded background refresh in flight. */
public final class ClientReportIndexService {
    private static final Duration CACHE_TTL = Duration.ofSeconds(20);
    private static final ClientReportIndexService INSTANCE = new ClientReportIndexService();

    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile LocalReportIndex.Catalog catalog = LocalReportIndex.Catalog.empty();
    private volatile boolean invalidated = true;

    private ClientReportIndexService() { }
    public static ClientReportIndexService instance() { return INSTANCE; }

    public LocalReportIndex.Catalog catalog() { return catalog; }
    public boolean refreshing() { return refreshing.get(); }
    public void invalidate() { invalidated = true; }

    /** Never blocks a render or tick; calls coalesce while the shared file lane is busy. */
    public void ensureFresh() {
        LocalReportIndex.Catalog current = catalog;
        if (!invalidated && !current.refreshedAt().equals(Instant.EPOCH)
                && current.refreshedAt().plus(CACHE_TTL).isAfter(Instant.now())) return;
        refresh();
    }

    public void refresh() {
        if (!refreshing.compareAndSet(false, true)) return;
        final List<LocalReportIndex.Root> roots = roots();
        try {
            GradleMcRuntimeExecutor.execute(GradleMcRuntimeExecutor.Lane.FILE_WORK, () -> {
                LocalReportIndex.Catalog completed = LocalReportIndex.scan(roots);
                Minecraft.getInstance().execute(() -> {
                    catalog = completed;
                    invalidated = false;
                    refreshing.set(false);
                });
            });
        } catch (RejectedExecutionException exception) {
            refreshing.set(false);
        }
    }

    public List<LocalReportIndex.Root> roots() {
        return LocalReportIndex.standardRoots(GradleMcPaths.reportDirectory(), GradleMcPaths.exportDirectory(),
                GradleMcPaths.profileDirectory(), GradleMcPaths.issueBundleDirectory());
    }
}

package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.SmartAIStatus;

public final class GradleMCGuiBridge {
    private static volatile Runnable clientOpener;
    private static volatile SmartAIStatus latestSmartAIStatus = SmartAIStatus.disabled();
    private static volatile GuiStatusSnapshot latestGuiStatus = GuiStatusSnapshot.empty();
    private static volatile long latestSmartAIStatusUpdatedAtMillis;

    private GradleMCGuiBridge() {
    }

    public static void registerClientOpener(Runnable opener) {
        clientOpener = opener;
    }

    public static void open() {
        if (clientOpener != null) {
            clientOpener.run();
        }
    }

    public static void updateSmartAIStatus(SmartAIStatus status) {
        updateStatus(status, latestGuiStatus);
    }

    public static void updateStatus(SmartAIStatus status, GuiStatusSnapshot guiStatus) {
        latestSmartAIStatus = status == null ? SmartAIStatus.disabled() : status;
        latestGuiStatus = guiStatus == null ? GuiStatusSnapshot.empty() : guiStatus;
        latestSmartAIStatusUpdatedAtMillis = System.currentTimeMillis();
    }

    public static SmartAIStatus latestSmartAIStatus() {
        return latestSmartAIStatus;
    }

    public static long smartAIStatusAgeMillis() {
        long updatedAt = latestSmartAIStatusUpdatedAtMillis;
        return updatedAt <= 0L ? -1L : Math.max(0L, System.currentTimeMillis() - updatedAt);
    }

    public static GuiStatusSnapshot latestGuiStatus() {
        return latestGuiStatus;
    }
}

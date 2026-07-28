package com.soumyajit.gradlemc.ai;

/** Deterministic digest proof: cooldown movement alone must not publish another packet. */
public final class AdaptiveSyncSelfTest {
    private AdaptiveSyncSelfTest() { }
    public static void run() {
        SmartAIStatus before = new SmartAIStatus(true, true, true, 8, ThreatLevel.LOW, "Collecting local behavior signals",
                200, 100, 0, 0, 0, 0, 0, 0, 0, 0, 100, 20, "no active pressure signals");
        SmartAIStatus timerOnly = new SmartAIStatus(true, true, true, 8, ThreatLevel.LOW, "Collecting local behavior signals",
                160, 60, 0, 0, 0, 0, 0, 0, 0, 0, 100, 20, "no active pressure signals");
        SmartAIStatus raised = new SmartAIStatus(true, true, true, 25, ThreatLevel.MEDIUM, "Collecting local behavior signals",
                160, 60, 0, 0, 0, 0, 0, 0, 0, 0, 100, 20, "damage pressure");
        require(AdaptiveSmartAIManager.materialDigest(before).equals(AdaptiveSmartAIManager.materialDigest(timerOnly)),
                "timer-only changes must not create a material update");
        require(!AdaptiveSmartAIManager.materialDigest(before).equals(AdaptiveSmartAIManager.materialDigest(raised)),
                "changed score or advice must create one material update");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

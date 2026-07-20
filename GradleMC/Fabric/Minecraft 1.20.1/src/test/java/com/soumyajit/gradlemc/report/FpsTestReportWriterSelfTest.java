package com.soumyajit.gradlemc.report;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.soumyajit.gradlemc.metrics.FpsTestResult;

import java.time.Instant;
import java.util.OptionalDouble;

public final class FpsTestReportWriterSelfTest {
    private FpsTestReportWriterSelfTest() {
    }

    public static void run() {
        FpsTestResult unavailable = new FpsTestResult(30, 0.0D, 0, null, null, null,
                OptionalDouble.empty(), Instant.EPOCH, Instant.EPOCH, FpsTestResult.EndReason.CANCELLED);
        JsonObject json = JsonParser.parseString(FpsTestReportWriter.jsonFor(unavailable)).getAsJsonObject();
        require("Fabric".equals(json.get("loader").getAsString()), "FPS JSON must retain Fabric identity");
        require(json.get("averageFps").isJsonNull(), "unavailable FPS must not be encoded as zero or NaN");
        require(json.get("samples").getAsInt() == 0, "sample count must remain numeric");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

package com.soumyajit.gradlemc.check;

import java.util.List;

public interface StabilityCheck {
    String name();

    List<CheckResult> run(CheckContext context);
}

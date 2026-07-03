package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.ModContainer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LoadedModsCheck implements StabilityCheck {
    @Override
    public String name() {
        return "Loaded mods";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        Collection<ModContainer> mods = QuiltLoader.getAllMods();
        String preview = mods.stream()
                .map(mod -> mod.metadata().id())
                .limit(8)
                .collect(Collectors.joining(", "));
        String detail = mods.size() + " loaded mods";
        if (!preview.isBlank()) {
            detail = detail + " (" + preview + ")";
        }
        return List.of(CheckResult.of(
                Severity.INFO,
                CheckCategory.MODS,
                "Loaded mod list is available",
                detail,
                "Future GradleMC rules can inspect this list for known risky combinations."
        ));
    }
}

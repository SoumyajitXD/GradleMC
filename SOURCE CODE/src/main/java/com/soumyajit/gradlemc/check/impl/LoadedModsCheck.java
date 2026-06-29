package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.List;
import java.util.stream.Collectors;

public class LoadedModsCheck implements StabilityCheck {
    @Override
    public String name() {
        return "Loaded mods";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        List<IModInfo> mods = ModList.get().getMods();
        String preview = mods.stream()
                .map(IModInfo::getModId)
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

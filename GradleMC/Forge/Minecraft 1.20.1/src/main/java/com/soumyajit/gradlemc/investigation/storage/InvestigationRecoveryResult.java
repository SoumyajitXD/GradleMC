package com.soumyajit.gradlemc.investigation.storage;
import java.util.*;
public record InvestigationRecoveryResult(InvestigationIndex index,boolean rebuilt,List<String> diagnostics){public InvestigationRecoveryResult{diagnostics=List.copyOf(diagnostics==null?List.of():diagnostics);}}

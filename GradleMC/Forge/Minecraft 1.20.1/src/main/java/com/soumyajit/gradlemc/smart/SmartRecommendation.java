package com.soumyajit.gradlemc.smart;

import java.util.List;

public record SmartRecommendation(String title,String reason,String action,int priority,ConfidenceLevel confidence,
                                  List<DiagnosticEvidence> evidence,List<String> evidenceIds,String affectedComponent,
                                  String hypothesis,String expectedMetric,String successCriterion,String rollback,String limitations) {
    public SmartRecommendation { evidence=List.copyOf(evidence);evidenceIds=List.copyOf(evidenceIds);limitations=limitations==null?"":limitations; }
    public SmartRecommendation(String title,String reason,String action,int priority,ConfidenceLevel confidence,List<DiagnosticEvidence> evidence){
        this(title,reason,action,priority,confidence,evidence,evidence.stream().map(e->"metric:"+e.metric()).toList(),
                evidence.isEmpty()?"diagnostic coverage":component(evidence.get(0).metric()),reason,
                evidence.isEmpty()?"requested diagnostic evidence":evidence.get(0).metric(),
                evidence.isEmpty()?"The requested diagnostic produces comparable evidence.":"The expected metric improves beyond its meaningful-change threshold.",
                "Undo only the reversible manual change being tested; diagnostic collection itself changes no instance content.",
                "Association is not causation; verify with a controlled before/after experiment.");
    }
    private static String component(String metric){int dot=metric.indexOf('.');return dot>0?metric.substring(0,dot):metric;}
}

package com.soumyajit.gradlemc.smart;

import java.util.*;

/** Suppresses unsafe/unsupported advice and explicitly downgrades coverage-only recommendations. */
public final class RecommendationValidator {
    private RecommendationValidator(){ }
    public record Validation(List<SmartRecommendation> recommendations,List<String> notes){public Validation{recommendations=List.copyOf(recommendations);notes=List.copyOf(notes);}}
    public static Validation validate(List<SmartRecommendation> input,List<String> missing){List<SmartRecommendation> output=new ArrayList<>();List<String> notes=new ArrayList<>();for(SmartRecommendation recommendation:input){String text=(recommendation.title()+" "+recommendation.reason()+" "+recommendation.action()).toLowerCase(Locale.ROOT);String rejected=null;
            if(text.contains("random mod")||text.contains("update everything")||text.contains("better hardware"))rejected="unsafe or non-diagnostic generic advice";
            else if(text.contains("increase heap")&&recommendation.evidence().stream().noneMatch(e->e.metric().toLowerCase(Locale.ROOT).contains("memory")))rejected="heap advice lacks memory evidence";
            else if((text.contains("disable shader")||text.contains("shaders off"))&&recommendation.evidence().stream().noneMatch(e->{String m=e.metric().toLowerCase(Locale.ROOT);return m.contains("fps")||m.contains("frame")||m.contains("shader");}))rejected="shader advice lacks shader/FPS evidence";
            if(rejected!=null){notes.add("Suppressed recommendation '"+recommendation.title()+"': "+rejected);continue;}
            if(recommendation.evidence().isEmpty()){DiagnosticEvidence gap=new DiagnosticEvidence("coverage.missing","unavailable","required before causal advice","Collect the requested metric in the same instance context.");output.add(new SmartRecommendation(recommendation.title(),recommendation.reason(),recommendation.action(),Math.min(recommendation.priority(),20),ConfidenceLevel.LOW,List.of(gap),List.of("coverage:missing"),"diagnostic coverage",recommendation.hypothesis(),gap.metric(),"Comparable evidence is collected.",recommendation.rollback(),"Evidence is currently missing; this recommends collection only."));notes.add("Downgraded coverage-only recommendation '"+recommendation.title()+"' to LOW confidence");}
            else output.add(recommendation);
        }return new Validation(output,notes);}
}

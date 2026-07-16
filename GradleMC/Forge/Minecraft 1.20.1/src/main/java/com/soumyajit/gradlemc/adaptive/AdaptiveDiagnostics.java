package com.soumyajit.gradlemc.adaptive;

import com.soumyajit.gradlemc.foundation.Evidence;
import com.soumyajit.gradlemc.foundation.Freshness;
import com.soumyajit.gradlemc.foundation.RuntimeContextFingerprint;
import com.soumyajit.gradlemc.foundation.StaticFingerprint;
import com.soumyajit.gradlemc.foundation.TaskCore;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Common-side, bounded, deterministic diagnostics evaluation. It never reads live game state. */
public final class AdaptiveDiagnostics {
    public static final int RECOMMENDATION_SCHEMA_VERSION = 1, RULE_SET_VERSION = 1, BASELINE_SCHEMA_VERSION = 1;
    public static final int MAX_EVIDENCE = 128, MAX_RULES = 16, MAX_RECOMMENDATIONS = 12, MAX_BASELINES = 32, MAX_RELATIONSHIPS = 64;
    private AdaptiveDiagnostics() { }
    public enum Context { MAIN_MENU, WORLD_IDLE, EXPLORATION, WORLD_GENERATION, INTEGRATED_SERVER, DEDICATED_SERVER, SHADER_ENABLED, SHADER_DISABLED, SHADER_UNKNOWN, UNKNOWN }
    public enum Eligibility { VALID, ABSENT, COLLECTOR_UNAVAILABLE, COLLECTOR_FAILED, STALE, CONTEXT_MISMATCH, TRUNCATED, WRONG_UNIT, WRONG_METHOD }
    public enum Comparability { COMPARABLE, PARTIALLY_COMPARABLE, NOT_COMPARABLE, INSUFFICIENT_CONTEXT, STATIC_INSTANCE_CHANGED, RUNTIME_CONTEXT_CHANGED, MEASUREMENT_METHOD_CHANGED, SAMPLE_DURATION_INCOMPATIBLE, STALE, TRUNCATED }
    public enum Priority { INFORMATIONAL, LOW, MEDIUM, HIGH, CRITICAL }
    public enum RecommendationState { PROPOSED, READY_TO_VERIFY, BLOCKED_BY_MISSING_EVIDENCE, VERIFIED_IMPROVED, VERIFIED_NO_IMPROVEMENT, VERIFIED_REGRESSED, INCONCLUSIVE, STALE, NOT_APPLICABLE, DISMISSED }
    public enum Confidence { NOT_APPLICABLE, LOW, MEDIUM, HIGH }
    public enum Direction { INCREASE, DECREASE, REMAIN_WITHIN_RANGE, RESOLVE_CONDITION }
    public enum RuleOutcome { MATCHED, NOT_MATCHED, NOT_APPLICABLE, INSUFFICIENT_EVIDENCE, STALE_EVIDENCE, CONTEXT_MISMATCH, TRUNCATED_INPUT, FAILED }
    public enum VerificationResult { IMPROVED, NO_MEANINGFUL_CHANGE, REGRESSED, INCONCLUSIVE, NOT_COMPARABLE, INCOMPLETE, FAILED }
    public record RuntimeContext(Set<Context> flags, String dimension, int renderDistance, int simulationDistance) {
        public RuntimeContext { flags=Set.copyOf(flags==null?Set.of(Context.UNKNOWN):flags); dimension=dimension==null?"unknown":dimension; }
        public boolean has(Context context) { return flags.contains(context); }
    }
    public record Baseline(String id, String metric, String unit, String method, StaticFingerprint staticFingerprint, RuntimeContextFingerprint runtimeFingerprint, RuntimeContext context, Instant createdAt, int samples, long durationSeconds, double median, double tail, List<String> limitations) {
        public Baseline { if(id==null||metric==null||unit==null||method==null||staticFingerprint==null||runtimeFingerprint==null||createdAt==null||samples<1||durationSeconds<0) throw new IllegalArgumentException("Invalid baseline"); limitations=List.copyOf(limitations==null?List.of():limitations); }
    }
    public record Comparison(Comparability decision, List<String> reasons) { public Comparison { reasons=List.copyOf(reasons==null?List.of():reasons); } }
    public record Prediction(String metric, String displayName, String unit, Direction direction, double minimumMeaningfulChange, double regressionGuardrail, String workflow) { }
    public record VerificationPlan(String id, List<String> preconditions, List<String> controls, List<String> steps, int durationSeconds, int repetitions, String success, String inconclusive, String rollback, List<String> limitations) {
        public VerificationPlan { preconditions=List.copyOf(preconditions);controls=List.copyOf(controls);steps=List.copyOf(steps);limitations=List.copyOf(limitations); if(id.isBlank()||success.isBlank()||inconclusive.isBlank()||rollback.isBlank()) throw new IllegalArgumentException("Verification plan requires criteria and rollback"); }
    }
    public record Recommendation(String id, int schemaVersion, String ruleSetVersion, String title, String summary, String category, Priority priority, RecommendationState state, Evidence.Classification classification, List<String> supportingEvidenceIds, List<String> contradictingEvidenceIds, List<String> missingEvidence, Confidence confidence, List<String> confidenceExplanation, String reversibleAction, Prediction prediction, VerificationPlan verification, List<String> limitations, StaticFingerprint staticFingerprint, RuntimeContextFingerprint runtimeFingerprint, List<String> ruleIds, Instant createdAt) {
        public Recommendation { supportingEvidenceIds=bounded(supportingEvidenceIds,16); contradictingEvidenceIds=bounded(contradictingEvidenceIds,16);missingEvidence=bounded(missingEvidence,16);confidenceExplanation=bounded(confidenceExplanation,16);limitations=bounded(limitations,16);ruleIds=bounded(ruleIds,8); if(schemaVersion!=RECOMMENDATION_SCHEMA_VERSION||id==null||!id.matches("[a-z0-9:_-]{3,96}")||title.isBlank()||summary.isBlank()||classification==Evidence.Classification.HYPOTHESIS&&(verification==null||limitations.isEmpty()))throw new IllegalArgumentException("Invalid recommendation"); }
    }
    public record RuleResult(String ruleId, RuleOutcome outcome, List<String> consumed, List<String> rejected, List<String> missing, Map<String,String> thresholds, Map<String,String> observed, List<String> limitations, String error) { public RuleResult {consumed=bounded(consumed,16);rejected=bounded(rejected,16);missing=bounded(missing,16);thresholds=Map.copyOf(thresholds==null?Map.of():new TreeMap<>(thresholds));observed=Map.copyOf(observed==null?Map.of():new TreeMap<>(observed));limitations=bounded(limitations,16);error=error==null?"":error;} }
    public record Evaluation(List<RuleResult> ruleResults, List<Recommendation> recommendations, List<String> missingEvidence, boolean truncated) { public Evaluation {ruleResults=List.copyOf(ruleResults);recommendations=List.copyOf(recommendations);missingEvidence=bounded(missingEvidence,32);} }
    public static Evaluation evaluate(List<Evidence> input, List<TaskCore.TaskResult> tasks, StaticFingerprint stat, RuntimeContextFingerprint runtime, RuntimeContext context, Instant now) {
        List<Evidence> evidence=input==null?List.of():input.stream().sorted(Comparator.comparing(Evidence::id)).limit(MAX_EVIDENCE).toList(); boolean truncated=input!=null&&input.size()>MAX_EVIDENCE;
        List<RuleResult> results=new ArrayList<>(); List<Recommendation> recommendations=new ArrayList<>();
        numericRule("memory:high_heap_pressure", "memory.pressure", "ratio", .85, "MEMORY", "High heap pressure observed", evidence, stat,runtime,now,results,recommendations);
        numericRule("performance:server_tick_pressure", "server.mspt", "ms", 50, "SERVER_TICK", "Elevated server tick time observed", evidence, stat,runtime,now,results,recommendations);
        numericRule("performance:high_frame_time_tail", "frame.p99", "ms", 50, "FRAME_TIME", "High frame-time tail observed", evidence, stat,runtime,now,results,recommendations);
        List<String> missing=tasks==null?List.of():tasks.stream().filter(t->t.state()!=TaskCore.State.SUCCEEDED).map(t->t.id()+":"+t.state()).sorted().limit(32).toList();
        if(truncated) results.add(new RuleResult("adaptive:truncated_input",RuleOutcome.TRUNCATED_INPUT,List.of(),List.of(),List.of(),Map.of(),Map.of(),List.of("Only the first "+MAX_EVIDENCE+" evidence records were evaluated."),""));
        return new Evaluation(results,recommendations.stream().limit(MAX_RECOMMENDATIONS).toList(),missing,truncated);
    }
    private static void numericRule(String id,String metric,String unit,double threshold,String category,String title,List<Evidence> evidence,StaticFingerprint stat,RuntimeContextFingerprint runtime,Instant now,List<RuleResult> results,List<Recommendation> out) {
        List<Evidence> candidates=evidence.stream().filter(e->metric.equals(e.fields().get("metric"))).toList();
        if(candidates.isEmpty()){results.add(new RuleResult(id,RuleOutcome.INSUFFICIENT_EVIDENCE,List.of(),List.of(),List.of(metric),Map.of("threshold",Double.toString(threshold),"unit",unit),Map.of(),List.of("No typed metric evidence was supplied."),""));return;}
        Evidence e=candidates.get(candidates.size()-1); Eligibility eligibility=eligibility(e,unit,now);
        if(eligibility!=Eligibility.VALID){ RuleOutcome outcome=eligibility==Eligibility.STALE?RuleOutcome.STALE_EVIDENCE:eligibility==Eligibility.CONTEXT_MISMATCH?RuleOutcome.CONTEXT_MISMATCH:RuleOutcome.INSUFFICIENT_EVIDENCE;results.add(new RuleResult(id,outcome,List.of(),List.of(e.id()),List.of(metric),Map.of("threshold",Double.toString(threshold)),Map.of(),List.of("Evidence is "+eligibility+"."),""));return; }
        double value; try {value=Double.parseDouble(e.fields().get("value"));} catch(Exception ex){results.add(new RuleResult(id,RuleOutcome.FAILED,List.of(e.id()),List.of(),List.of(),Map.of(),Map.of(),List.of(),"Metric value was not numeric"));return;}
        if(value<threshold){results.add(new RuleResult(id,RuleOutcome.NOT_MATCHED,List.of(e.id()),List.of(),List.of(),Map.of("threshold",Double.toString(threshold)),Map.of("value",Double.toString(value)),List.of(),""));return;}
        Prediction prediction=new Prediction(metric,metric,unit,Direction.DECREASE,threshold*.10,threshold*.10,"Repeat the same bounded measurement under the same runtime context.");
        VerificationPlan plan=new VerificationPlan("verify:"+id,List.of("Capture a fresh baseline in the same context."),List.of("Keep shader state, render distance, dimension, and route unchanged."),List.of("Make one manual, reversible change.","Repeat the measurement for at least 60 seconds.","Compare the named metric only when contexts are comparable."),60,2,"Metric decreases by the meaningful-change threshold without a related regression.","Samples differ in context, duration, or are too noisy.","Restore the manually changed setting if there is no benefit.",List.of("This rule observes a threshold; it does not establish cause."));
        Confidence confidence=e.classification()==Evidence.Classification.OBSERVED_FACT?Confidence.MEDIUM:Confidence.LOW;
        Recommendation r=new Recommendation("rec:"+id,RECOMMENDATION_SCHEMA_VERSION,"adaptive-rules-v"+RULE_SET_VERSION,title,"A declared threshold matched; the evidence does not isolate causation.",category,Priority.MEDIUM,RecommendationState.READY_TO_VERIFY,Evidence.Classification.RULE_MATCH,List.of(e.id()),List.of(),List.of(),confidence,List.of("Fresh typed measurement.","Deterministic threshold matched.","One observation does not prove cause."),"Manually inspect the affected setting or workload, then run the controlled verification plan.",prediction,plan,List.of("Correlation or threshold matching is not attribution.","No automatic change is performed."),stat,runtime,List.of(id),now);
        results.add(new RuleResult(id,RuleOutcome.MATCHED,List.of(e.id()),List.of(),List.of(),Map.of("threshold",Double.toString(threshold)),Map.of("value",Double.toString(value)),r.limitations(),""));out.add(r);
    }
    public static Eligibility eligibility(Evidence evidence,String unit,Instant now){if(evidence==null)return Eligibility.ABSENT;if(evidence.freshness().state()==Freshness.State.CONTEXT_MISMATCH)return Eligibility.CONTEXT_MISMATCH;if(evidence.freshness().state()!=Freshness.State.FRESH)return Eligibility.STALE;if(!unit.equals(evidence.fields().get("unit")))return Eligibility.WRONG_UNIT;if(evidence.fields().containsKey("truncated"))return Eligibility.TRUNCATED;return Eligibility.VALID;}
    public static Comparison compare(Baseline baseline,String metric,String unit,String method,StaticFingerprint stat,RuntimeContextFingerprint runtime,Instant now){if(baseline==null)return new Comparison(Comparability.INSUFFICIENT_CONTEXT,List.of("No baseline available."));if(!baseline.metric().equals(metric)||!baseline.unit().equals(unit))return new Comparison(Comparability.NOT_COMPARABLE,List.of("Metric or unit differs."));if(!baseline.method().equals(method))return new Comparison(Comparability.MEASUREMENT_METHOD_CHANGED,List.of("Measurement method differs."));if(!baseline.staticFingerprint().equals(stat))return new Comparison(Comparability.STATIC_INSTANCE_CHANGED,List.of("Static instance fingerprint differs."));if(!baseline.runtimeFingerprint().equals(runtime))return new Comparison(Comparability.RUNTIME_CONTEXT_CHANGED,List.of("Runtime context fingerprint differs."));if(Duration.between(baseline.createdAt(),now).toDays()>30)return new Comparison(Comparability.STALE,List.of("Baseline is older than 30 days."));if(baseline.samples()<2)return new Comparison(Comparability.PARTIALLY_COMPARABLE,List.of("Baseline has fewer than two samples."));return new Comparison(Comparability.COMPARABLE,List.of("Metric, method, static fingerprint, and runtime context match."));}
    private static List<String> bounded(List<String> values,int max){if(values==null)return List.of();if(values.size()>max)throw new IllegalArgumentException("Bound exceeded");return List.copyOf(values);}
}

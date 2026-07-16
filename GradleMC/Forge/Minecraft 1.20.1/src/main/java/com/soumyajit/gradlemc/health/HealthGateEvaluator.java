package com.soumyajit.gradlemc.health;

import com.soumyajit.gradlemc.task.TaskState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HealthGateEvaluator {
    private HealthGateEvaluator() { }

    public static List<HealthGateResult> evaluate(List<HealthGate> gates, HealthGateEvidence evidence) {
        List<HealthGateResult> results = new ArrayList<>();
        gates.stream().sorted(Comparator.comparing(HealthGate::id)).forEach(gate -> results.add(evaluate(gate, evidence)));
        return List.copyOf(results);
    }

    private static HealthGateResult evaluate(HealthGate gate, HealthGateEvidence evidence) {
        if (!gate.enabled()) return result(gate, HealthGateState.NOT_EVALUATED, "Gate is disabled", null, evidence);
        if (gate.kind() == HealthGateKind.REQUIRED_TASK) {
            TaskState state = evidence.taskStates().get(gate.evidenceKey());
            if (state == null) return result(gate, HealthGateState.INCONCLUSIVE, "Required task has no evidence", null, evidence);
            boolean passed = state == TaskState.SUCCESS || state == TaskState.UP_TO_DATE;
            return result(gate, passed ? HealthGateState.PASS : HealthGateState.FAIL,
                    passed ? "Required task completed" : "Required task state is " + state, null, evidence);
        }
        Double observed = evidence.metrics().get(gate.evidenceKey());
        if (observed == null || !Double.isFinite(observed)) {
            return result(gate, HealthGateState.INCONCLUSIVE, "Required metric evidence is missing or non-finite", null, evidence);
        }
        boolean passed = gate.kind() == HealthGateKind.MINIMUM ? observed >= gate.threshold() : observed <= gate.threshold();
        String comparison = gate.kind() == HealthGateKind.MINIMUM ? ">=" : "<=";
        return result(gate, passed ? HealthGateState.PASS : HealthGateState.FAIL,
                "Observed " + observed + " " + comparison + " threshold " + gate.threshold(), observed, evidence);
    }

    private static HealthGateResult result(HealthGate gate, HealthGateState state, String explanation,
                                           Double observed, HealthGateEvidence evidence) {
        String id = evidence.evidenceIds().get(gate.evidenceKey());
        return new HealthGateResult(gate.id(), state, explanation, observed, gate.threshold(), id == null ? List.of() : List.of(id));
    }
}

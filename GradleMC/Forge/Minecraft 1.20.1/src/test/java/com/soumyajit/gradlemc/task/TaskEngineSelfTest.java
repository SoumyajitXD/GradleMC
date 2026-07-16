package com.soumyajit.gradlemc.task;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class TaskEngineSelfTest {
    private TaskEngineSelfTest() { }
    public static void run() {
        orderingAndStaticReuseAreDeterministic();
        duplicateAndCyclesAreRejected();
        failedRequiredDependencyBlocksDependent();
        optionalMissingDependencyDoesNotBreakPlanning();
        dynamicTasksNeverReuseAndChangesAreExplained();
        timeoutAndBudgetStatesAreExplicit();
        identifiersAndStandardWorkflowAreLowercase();
    }
    private static void optionalMissingDependencyDoesNotBreakPlanning() {
        TaskEngine engine = new TaskEngine();
        engine.register(task("gradlemc:a", List.of(TaskDependency.optional("gradlemc:not_registered")), CachePolicy.NEVER_CACHE, TaskOutcome.success(Map.of())));
        assertEquals(List.of("gradlemc:a"), engine.plan("gradlemc:a").orderedTasks().stream().map(DiagnosticTask::id).toList(), "optional missing dependency");
    }
    private static void dynamicTasksNeverReuseAndChangesAreExplained() {
        TaskEngine engine = new TaskEngine(); AtomicInteger input = new AtomicInteger(1), executions = new AtomicInteger();
        DiagnosticTask dynamic = new DiagnosticTask() {
            public String id(){return "gradlemc:dynamic";} public String displayName(){return id();} public String description(){return id();} public String group(){return "test";} public String version(){return "1";}
            public List<TaskDependency> dependencies(){return List.of();} public CachePolicy cachePolicy(){return CachePolicy.NEVER_CACHE;} public boolean requiresServer(){return false;} public long timeoutMillis(){return 1000;}
            public Map<String,String> inputs(TaskRunContext c){return Map.of("value",Integer.toString(input.get()));} public TaskOutcome execute(TaskRunContext c){executions.incrementAndGet();return TaskOutcome.success(Map.of());}
        };
        engine.register(dynamic); TaskPlan plan=engine.plan(dynamic.id()); engine.execute(plan,new TaskRunContext(null,false),false); engine.execute(plan,new TaskRunContext(null,false),false);
        assertEquals(2,executions.get(),"dynamic task execution count");
        TaskExplanation explanation=engine.explain(dynamic.id(),dynamic.id(),new TaskRunContext(null,false)); assertEquals(TaskState.PLANNED,explanation.predictedState(),"dynamic why state");
        input.incrementAndGet(); assertEquals("1 -> 2",engine.explain(dynamic.id(),dynamic.id(),new TaskRunContext(null,false)).changedInputs().get("value"),"changed input explanation");
    }
    private static void timeoutAndBudgetStatesAreExplicit() {
        TaskEngine timeoutEngine=new TaskEngine(); timeoutEngine.register(new DiagnosticTask(){public String id(){return "gradlemc:slow";}public String displayName(){return id();}public String description(){return id();}public String group(){return "test";}public String version(){return "1";}public List<TaskDependency> dependencies(){return List.of();}public CachePolicy cachePolicy(){return CachePolicy.NEVER_CACHE;}public boolean requiresServer(){return false;}public long timeoutMillis(){return 1;}public Map<String,String> inputs(TaskRunContext c){return Map.of();}public TaskOutcome execute(TaskRunContext c)throws Exception{Thread.sleep(10);return TaskOutcome.success(Map.of());}});
        TaskResult timed=timeoutEngine.execute(timeoutEngine.plan("gradlemc:slow"),new TaskRunContext(null,false),false).get(0); assertEquals(TaskState.TIMED_OUT,timed.state(),"timeout state"); assertEquals(true,timed.overhead().timedOut(),"timeout overhead");
        TaskEngine budgetEngine=new TaskEngine(); budgetEngine.register(task("gradlemc:a",List.of(),CachePolicy.NEVER_CACHE,TaskOutcome.success(Map.of()))); budgetEngine.register(task("gradlemc:b",List.of(TaskDependency.required("gradlemc:a")),CachePolicy.NEVER_CACHE,TaskOutcome.success(Map.of())));
        List<TaskResult> values=budgetEngine.execute(budgetEngine.plan("gradlemc:b"),new TaskRunContext(null,false,new ExecutionBudget(1,1000,1,1,1,1,1024)),false); assertEquals("budget-reached",values.get(1).reason(),"task count budget");
    }
    private static void orderingAndStaticReuseAreDeterministic() {
        TaskEngine engine = new TaskEngine();
        engine.register(task("gradlemc:a", List.of(), CachePolicy.STATIC_INPUTS, TaskOutcome.success(Map.of("a", "1"))));
        engine.register(task("gradlemc:b", List.of(TaskDependency.required("gradlemc:a")), CachePolicy.NEVER_CACHE, TaskOutcome.success(Map.of())));
        TaskPlan plan = engine.plan("gradlemc:b"); assertEquals(List.of("gradlemc:a", "gradlemc:b"), plan.orderedTasks().stream().map(DiagnosticTask::id).toList(), "topological order");
        engine.execute(plan, new TaskRunContext(null, false), false);
        assertEquals(TaskState.UP_TO_DATE, engine.execute(plan, new TaskRunContext(null, false), false).get(0).state(), "static task should reuse matching inputs");
        assertEquals(TaskEngine.fingerprint(Map.of("b", "2", "a", "1")), TaskEngine.fingerprint(Map.of("a", "1", "b", "2")), "fingerprint ordering");
    }
    private static void duplicateAndCyclesAreRejected() {
        TaskEngine engine = new TaskEngine(); engine.register(task("gradlemc:a", List.of(TaskDependency.required("gradlemc:b")), CachePolicy.NEVER_CACHE, TaskOutcome.success(Map.of()))); engine.register(task("gradlemc:b", List.of(TaskDependency.required("gradlemc:a")), CachePolicy.NEVER_CACHE, TaskOutcome.success(Map.of())));
        expect(() -> engine.plan("gradlemc:a"), "cycle"); expect(() -> engine.register(task("gradlemc:a", List.of(), CachePolicy.NEVER_CACHE, TaskOutcome.success(Map.of()))), "Duplicate");
        expect(() -> engine.register(task("gradlemc:Upper", List.of(), CachePolicy.NEVER_CACHE, TaskOutcome.success(Map.of()))), "Invalid namespaced");
    }
    private static void identifiersAndStandardWorkflowAreLowercase() {
        TaskEngine engine = new TaskEngine(); BuiltinTasks.register(engine);
        if (!DiagnosticWorkflows.isFoundationWorkflow("standard")) throw new AssertionError("standard Scan workflow is not routed to Scan v1");
        if (!DiagnosticWorkflows.ids().contains("standard")) throw new AssertionError("standard workflow is not listed");
        for (String id : DiagnosticWorkflows.ids()) if (!id.equals(id.toLowerCase(java.util.Locale.ROOT))) throw new AssertionError("uppercase workflow ID: " + id);
        for (DiagnosticTask task : engine.tasks()) if (!task.id().equals(task.id().toLowerCase(java.util.Locale.ROOT))) throw new AssertionError("uppercase task ID: " + task.id());
    }
    private static void failedRequiredDependencyBlocksDependent() {
        TaskEngine engine = new TaskEngine(); engine.register(task("gradlemc:a", List.of(), CachePolicy.NEVER_CACHE, TaskOutcome.failed("test", "failed"))); engine.register(task("gradlemc:b", List.of(TaskDependency.required("gradlemc:a")), CachePolicy.NEVER_CACHE, TaskOutcome.success(Map.of())));
        assertEquals(TaskState.SKIPPED, engine.execute(engine.plan("gradlemc:b"), new TaskRunContext(null, false), false).get(1).state(), "failed required dependency blocks task");
    }
    private static DiagnosticTask task(String id, List<TaskDependency> deps, CachePolicy cache, TaskOutcome outcome) { return new DiagnosticTask() { public String id(){return id;} public String displayName(){return id;} public String description(){return id;} public String group(){return "test";} public String version(){return "1";} public List<TaskDependency> dependencies(){return deps;} public CachePolicy cachePolicy(){return cache;} public boolean requiresServer(){return false;} public long timeoutMillis(){return 1_000;} public Map<String,String> inputs(TaskRunContext c){return Map.of("id",id);} public TaskOutcome execute(TaskRunContext c){return outcome;} }; }
    private static void expect(Runnable r,String part){try{r.run();throw new AssertionError("Expected failure");}catch(IllegalArgumentException e){if(!e.getMessage().contains(part))throw e;}}
    private static void assertEquals(Object expected,Object actual,String message){if(!expected.equals(actual))throw new AssertionError(message+" expected="+expected+" actual="+actual);}
}

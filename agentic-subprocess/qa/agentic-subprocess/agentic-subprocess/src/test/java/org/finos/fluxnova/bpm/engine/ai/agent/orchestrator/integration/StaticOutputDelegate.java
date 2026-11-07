package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.integration;

import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.JavaDelegate;

import java.util.ArrayList;
import java.util.List;

/**
 * Records each executed activity id in the {@code executedActivities} process variable (and
 * increments {@code loopIterations} when present). Used by integration tests as a tool/step
 * implementation inside agent ad-hoc subprocesses.
 */
public class StaticOutputDelegate implements JavaDelegate {

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        Object raw = execution.getVariable("executedActivities");
        List<String> executed =
                raw instanceof List<?> list ? new ArrayList<>((List<String>) list) : new ArrayList<>();
        executed.add(activityId);
        execution.setVariable("executedActivities", executed);

        Object loopCount = execution.getVariable("loopIterations");
        if (loopCount instanceof Integer count && "loopStep".equals(activityId)) {
            execution.setVariable("loopIterations", count + 1);
        }
    }
}

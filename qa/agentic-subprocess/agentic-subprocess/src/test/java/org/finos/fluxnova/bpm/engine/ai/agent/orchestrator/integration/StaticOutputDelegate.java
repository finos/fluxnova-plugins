package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.integration;

import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.JavaDelegate;

/**
 * Writes a static output variable named {@code toolOutput} whose value is
 * {@code "result from <activityId>"}. Used by integration tests as a trivial tool implementation
 * inside agent ad-hoc subprocesses.
 */
public class StaticOutputDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        // No-op: the orchestration tests only care that activities are triggered
        // and complete, not about their output.
    }
}

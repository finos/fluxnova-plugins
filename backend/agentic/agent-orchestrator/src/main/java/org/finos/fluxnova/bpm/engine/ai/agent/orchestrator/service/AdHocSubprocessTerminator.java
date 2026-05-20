package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

import org.finos.fluxnova.bpm.engine.RuntimeService;

public class AdHocSubprocessTerminator implements AgentTerminationHandler {

    private final RuntimeService runtimeService;

    public AdHocSubprocessTerminator(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void complete(String scopeExecutionId) {
        runtimeService.completeAdHocSubProcess(scopeExecutionId);
    }
}

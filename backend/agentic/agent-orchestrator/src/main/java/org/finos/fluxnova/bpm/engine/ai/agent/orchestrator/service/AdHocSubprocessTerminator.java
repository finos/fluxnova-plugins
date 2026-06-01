package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.springframework.beans.factory.ObjectProvider;

public class AdHocSubprocessTerminator implements AgentTerminationHandler {

    private final ObjectProvider<RuntimeService> runtimeService;

    public AdHocSubprocessTerminator(ObjectProvider<RuntimeService> runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void complete(String scopeExecutionId) {
        runtimeService.getObject().completeAdHocSubProcess(scopeExecutionId);
    }
}
package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

import org.finos.fluxnova.bpm.engine.RuntimeService;

public class AdHocSubprocessTerminator implements AgentTerminationHandler {

    public AdHocSubprocessTerminator() {
    }

    @Override
    public void complete(RuntimeService runtimeService, String scopeExecutionId) {
        runtimeService.completeAdHocSubProcess(scopeExecutionId);
    }
}

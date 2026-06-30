package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdHocSubprocessTerminator implements AgentTerminationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AdHocSubprocessTerminator.class);

    public AdHocSubprocessTerminator() {
    }

    @Override
    public void complete(RuntimeService runtimeService, String scopeExecutionId) {
        LOG.debug("complete() called for scope '{}'", scopeExecutionId);
        runtimeService.completeAdHocSubProcess(scopeExecutionId);
    }
}
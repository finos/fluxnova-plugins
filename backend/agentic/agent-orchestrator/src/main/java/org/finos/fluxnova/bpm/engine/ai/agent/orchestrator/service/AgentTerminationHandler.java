package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

/**
 * Strategy for completing a scope execution when its work is done.
 *
 * <p>Implementations define how a specific type of scope (e.g. an ad-hoc subprocess)
 * signals completion to the process engine. Alternative implementations can be
 * registered as Spring beans to support different scope types.
 */
public interface AgentTerminationHandler {

    /**
     * Completes the given scope execution.
     *
     * @param scopeExecutionId the id of the scope execution to complete
     */
    void complete(String scopeExecutionId);
}

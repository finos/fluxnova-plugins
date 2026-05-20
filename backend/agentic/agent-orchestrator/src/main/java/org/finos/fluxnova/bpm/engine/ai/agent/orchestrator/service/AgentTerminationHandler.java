package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

/**
 * Strategy for completing the agent scope execution. This interface allows different agentic
 * components (ad-hoc subprocess, event subprocess, etc.) to define how they complete the scope.
 */
public interface AgentTerminationHandler {

    void complete(String scopeExecutionId);
}

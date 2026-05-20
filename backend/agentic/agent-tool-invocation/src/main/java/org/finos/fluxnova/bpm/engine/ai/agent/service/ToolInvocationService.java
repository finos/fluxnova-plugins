package org.finos.fluxnova.bpm.engine.ai.agent.service;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.finos.fluxnova.bpm.engine.shared.model.ToolInvocationResult;

public interface ToolInvocationService {
    ToolInvocationResult invoke(String adHocSubprocessId, AgentToolCatalogue catalogue, ToolCallRequest request);
}

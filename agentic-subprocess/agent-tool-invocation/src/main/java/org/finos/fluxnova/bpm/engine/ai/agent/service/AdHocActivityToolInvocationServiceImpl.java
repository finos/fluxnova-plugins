package org.finos.fluxnova.bpm.engine.ai.agent.service;

import org.finos.fluxnova.bpm.engine.BadUserRequestException;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.finos.fluxnova.bpm.engine.shared.model.ToolInvocationResult;

import java.util.List;
import java.util.Map;

public class AdHocActivityToolInvocationServiceImpl implements ToolInvocationService {

    public AdHocActivityToolInvocationServiceImpl() {
    }

    @Override
    public ToolInvocationResult invoke(RuntimeService runtimeService, String adHocSubprocessId, AgentToolCatalogue catalogue, ToolCallRequest request) {
        if (catalogue.findById(request.toolId()).isEmpty()) {
            return ToolInvocationResult.failure(
                    request.toolCallId(),
                    "Unknown tool: " + request.toolId()
            );
        }

        try {
            Map<String, Map<String, Object>> variables = Map.of(request.toolId(), Map.of("_agentToolCallId", request.toolCallId()));
            runtimeService.triggerAdHocActivities(adHocSubprocessId, List.of(request.toolId()), variables);
            return ToolInvocationResult.success(request.toolCallId());
        } catch (BadUserRequestException e) {
            return ToolInvocationResult.failure(request.toolCallId(), e.getMessage());
        }
    }
}

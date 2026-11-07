package org.finos.fluxnova.bpm.engine.ai.agent.service;

import org.finos.fluxnova.bpm.engine.BadUserRequestException;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolParameter;
import org.finos.fluxnova.bpm.engine.ai.agent.el.AgentElFunctions;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.finos.fluxnova.bpm.engine.shared.model.ToolInvocationResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdHocActivityToolInvocationServiceImpl implements ToolInvocationService {

    public AdHocActivityToolInvocationServiceImpl() {
    }

    @Override
    public ToolInvocationResult invoke(RuntimeService runtimeService, String adHocSubprocessId, AgentToolCatalogue catalogue, ToolCallRequest request) {
        Optional<AgentToolEntry> toolOpt = catalogue.findById(request.toolId());
        if (toolOpt.isEmpty()) {
            return ToolInvocationResult.failure(
                    request.toolCallId(),
                    "Unknown tool: " + request.toolId()
            );
        }

        AgentToolEntry tool = toolOpt.get();
        Map<String, Object> activityVariables = buildActivityVariables(tool, request);

        try {
            Map<String, Map<String, Object>> variables = Map.of(request.toolId(), activityVariables);
            runtimeService.triggerAdHocActivities(adHocSubprocessId, List.of(request.toolId()), variables);
            return ToolInvocationResult.success(request.toolCallId());
        } catch (BadUserRequestException e) {
            return ToolInvocationResult.failure(request.toolCallId(), e.getMessage());
        }
    }

    private Map<String, Object> buildActivityVariables(AgentToolEntry tool, ToolCallRequest request) {
        if (request.arguments().isEmpty() || tool.parameters().isEmpty()) {
            return Map.of("_agentToolCallId", request.toolCallId());
        }

        Map<String, Object> declaredArgs = new LinkedHashMap<>();
        for (AgentToolParameter parameter : tool.parameters()) {
            if (request.arguments().containsKey(parameter.name())) {
                declaredArgs.put(parameter.name(), request.arguments().get(parameter.name()));
            }
        }

        if (declaredArgs.isEmpty()) {
            return Map.of("_agentToolCallId", request.toolCallId());
        }

        Map<String, Object> activityVars = new LinkedHashMap<>();
        activityVars.put("_agentToolCallId", request.toolCallId());
        activityVars.put(AgentElFunctions.VAR_TOOL_ARGS, Collections.unmodifiableMap(declaredArgs));
        return Collections.unmodifiableMap(activityVars);
    }
}

package org.finos.fluxnova.bpm.engine.ai.agent.service;

import org.finos.fluxnova.bpm.engine.BadUserRequestException;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure.AgentToolInvocationEnginePlugin;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolParameter;
import org.finos.fluxnova.bpm.engine.ai.agent.el.AgentElFunctions;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.el.Expression;
import org.finos.fluxnova.bpm.engine.impl.el.JuelExpressionManager;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.finos.fluxnova.bpm.engine.shared.model.ToolInvocationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdHocActivityToolInvocationServiceImplTest {

    @Test
    void invoke_withValidToolId_returnsSuccessAndCallsTool() {
        String toolId = "toolId";

        String scopeExecutionId = "scopeExecutionId";

        String processExecutionId = "processExecutionId";
        List<AgentToolEntry> agentToolEntries = List.of(
                new AgentToolEntry(toolId, "toolName", "toolDescription", Set.of("reads"), Set.of("writes")));
        AgentToolCatalogue catalogue = new AgentToolCatalogue(processExecutionId, toolId, agentToolEntries);

        String toolCallId = "toolCallId";
        ToolCallRequest request = new ToolCallRequest(toolCallId, toolId);

        RuntimeService runtimeService = mock(RuntimeService.class);
        ToolInvocationService toolInvocationService = new AdHocActivityToolInvocationServiceImpl();

        ToolInvocationResult expectedResult = ToolInvocationResult.success(toolCallId);

        ToolInvocationResult result = toolInvocationService.invoke(runtimeService, scopeExecutionId, catalogue, request);

        assertEquals(expectedResult, result);
        verify(runtimeService).triggerAdHocActivities(
                eq(scopeExecutionId),
                eq(List.of(toolId)),
                eq(Map.of(toolId, Map.of("_agentToolCallId", toolCallId)))
        );
    }

    @Test
    void invoke_withEmptyCatalogueTools_returnsFailure() {
        String toolId = "toolId";

        String scopeExecutionId = "scopeExecutionId";

        String processExecutionId = "processExecutionId";
        List<AgentToolEntry> agentToolEntries = List.of();
        AgentToolCatalogue catalogue = new AgentToolCatalogue(processExecutionId, toolId, agentToolEntries);

        String toolCallId = "toolCallId";
        ToolCallRequest request = new ToolCallRequest(toolCallId, toolId);

        RuntimeService runtimeService = mock(RuntimeService.class);
        ToolInvocationService toolInvocationService = new AdHocActivityToolInvocationServiceImpl();

        ToolInvocationResult expectedResult = ToolInvocationResult.failure(toolCallId, "Unknown tool: " + toolId);

        ToolInvocationResult result = toolInvocationService.invoke(runtimeService, scopeExecutionId, catalogue, request);

        assertEquals(expectedResult, result);
    }

    @Test
    void invoke_withToolIdNotInCatalogue_returnsFailure() {
        String toolId = "toolId";

        String scopeExecutionId = "scopeExecutionId";

        String processExecutionId = "processExecutionId";
        List<AgentToolEntry> agentToolEntries = List.of(
                new AgentToolEntry("toolId2", "toolName2", "toolDescription2", Set.of("reads2"), Set.of("writes2")),
                new AgentToolEntry("toolId3", "toolName3", "toolDescription3", Set.of("reads3"), Set.of("writes3")));
        AgentToolCatalogue catalogue = new AgentToolCatalogue(processExecutionId, toolId, agentToolEntries);

        String toolCallId = "toolCallId";
        ToolCallRequest request = new ToolCallRequest(toolCallId, toolId);

        RuntimeService runtimeService = mock(RuntimeService.class);
        ToolInvocationService toolInvocationService = new AdHocActivityToolInvocationServiceImpl();

        ToolInvocationResult expectedResult = ToolInvocationResult.failure(toolCallId, "Unknown tool: " + toolId);

        ToolInvocationResult result = toolInvocationService.invoke(runtimeService, scopeExecutionId, catalogue, request);

        assertEquals(expectedResult, result);
    }

    @Test
    void invoke_withBadUserRequestException_returnsFailure() {
        String toolId = "toolId";

        String scopeExecutionId = "scopeExecutionId";

        String processExecutionId = "processExecutionId";
        List<AgentToolEntry> agentToolEntries = List.of(
                new AgentToolEntry(toolId, "toolName", "toolDescription", Set.of("reads"), Set.of("writes")));
        AgentToolCatalogue catalogue = new AgentToolCatalogue(processExecutionId, toolId, agentToolEntries);

        String toolCallId = "toolCallId";
        ToolCallRequest request = new ToolCallRequest(toolCallId, toolId);

        RuntimeService runtimeService = mock(RuntimeService.class);
        String errorMessage = "Bad user request exception";
        doThrow(new BadUserRequestException(errorMessage)).when(runtimeService).triggerAdHocActivities(any(), any(), any());
        ToolInvocationService toolInvocationService = new AdHocActivityToolInvocationServiceImpl();

        ToolInvocationResult expectedResult = ToolInvocationResult.failure(toolCallId, errorMessage);

        ToolInvocationResult result = toolInvocationService.invoke(runtimeService, scopeExecutionId, catalogue, request);

        assertEquals(expectedResult, result);
    }

    @Test
    void invoke_withDeclaredParameters_passesDeclaredArgumentsAndIgnoresUndeclaredKeys() {
        String toolId = "searchDocs";
        String scopeExecutionId = "scopeExecutionId";
        String processExecutionId = "processExecutionId";
        List<AgentToolEntry> agentToolEntries = List.of(
                new AgentToolEntry(
                        toolId,
                        "Search Docs",
                        "Searches documents",
                        Set.of(),
                        Set.of("results"),
                        List.of(new AgentToolParameter("query", "Search query"))));
        AgentToolCatalogue catalogue = new AgentToolCatalogue(processExecutionId, toolId, agentToolEntries);

        String toolCallId = "call-99";
        ToolCallRequest request = new ToolCallRequest(
                toolCallId,
                toolId,
                Map.of("query", "fluxnova", "undeclaredKey", "ignoredValue"));

        RuntimeService runtimeService = mock(RuntimeService.class);
        ToolInvocationService toolInvocationService = new AdHocActivityToolInvocationServiceImpl();

        ToolInvocationResult result = toolInvocationService.invoke(runtimeService, scopeExecutionId, catalogue, request);

        assertEquals(ToolInvocationResult.success(toolCallId), result);
        verify(runtimeService).triggerAdHocActivities(
                eq(scopeExecutionId),
                eq(List.of(toolId)),
                eq(Map.of(
                        toolId,
                        Map.of(
                                "_agentToolCallId", toolCallId,
                                AgentElFunctions.VAR_TOOL_ARGS, Map.of("query", "fluxnova")
                        )))
        );
    }

    @Test
    void agentArgElFunction_whenRegisteredViaPlugin_resolvesArgumentFromExecutionContext() {
        JuelExpressionManager expressionManager = new JuelExpressionManager();
        ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
        when(config.getExpressionManager()).thenReturn(expressionManager);
        when(config.getDelegateInterceptor()).thenReturn(invocation -> invocation.proceed());

        AgentToolInvocationEnginePlugin plugin = new AgentToolInvocationEnginePlugin();
        plugin.postInit(config);

        ExecutionEntity execution = mock(ExecutionEntity.class);
        when(execution.getVariableLocal(AgentElFunctions.VAR_TOOL_ARGS))
                .thenReturn(Map.of("query", "credit policy"));

        Context.setProcessEngineConfiguration(config);
        Context.setExecutionContext(execution);
        try {
            Expression expression = expressionManager.createExpression(
                    "searching: ${agent:arg('query', 'Search terms')}");
            Object evaluated = expression.getValue(execution);
            assertEquals("searching: credit policy", evaluated);
        } finally {
            Context.removeExecutionContext();
            Context.removeProcessEngineConfiguration();
        }
    }
}

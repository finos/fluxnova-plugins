package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AgentStateManager {

    private static final String VAR_CONVERSATION_HISTORY = "_agentConversationHistory";
    private static final String VAR_PENDING_TOOL_CALLS = "_agentPendingToolCalls";
    private static final String VAR_TOOL_RESULT_BUFFER = "_agentToolResultBuffer";
    private static final String VAR_TOOL_CALL_QUEUE = "_agentToolCallQueue";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<ConversationEntry>> HISTORY_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<Set<String>> PENDING_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<ToolResult>> RESULT_BUFFER_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<List<ToolCallRequest>> QUEUE_TYPE = new TypeReference<>() {};

    private final ObjectProvider<RuntimeService> runtimeService;

    public AgentStateManager(ObjectProvider<RuntimeService> runtimeService) {
        this.runtimeService = runtimeService;
    }

    public List<ConversationEntry> loadHistory(String executionId) {
        String json =
                (String) runtimeService.getObject().getVariableLocal(executionId, VAR_CONVERSATION_HISTORY);
        if (json == null) {
            return new ArrayList<>();
        }
        return deserialize(json, HISTORY_TYPE);
    }

    public void saveHistory(String executionId, List<ConversationEntry> history) {
        runtimeService.getObject().setVariableLocal(executionId, VAR_CONVERSATION_HISTORY, serialize(history));
    }

    public void savePendingToolCalls(String executionId, Set<String> pending) {
        runtimeService.getObject().setVariableLocal(executionId, VAR_PENDING_TOOL_CALLS, serialize(pending));
    }

    public boolean isPendingToolCall(String executionId, String toolCallId) {
        return loadPendingToolCalls(executionId).contains(toolCallId);
    }

    public boolean completeToolCall(String executionId, String toolCallId) {
        Set<String> pending = loadPendingToolCalls(executionId);
        pending.remove(toolCallId);
        savePendingToolCalls(executionId, pending);
        return pending.isEmpty();
    }

    public List<ToolResult> loadToolResultBuffer(String executionId) {
        String json = (String) runtimeService.getObject().getVariableLocal(executionId, VAR_TOOL_RESULT_BUFFER);
        if (json == null) {
            return new ArrayList<>();
        }
        return deserialize(json, RESULT_BUFFER_TYPE);
    }

    public void appendToResultBuffer(String executionId, ToolResult result) {
        List<ToolResult> buffer = loadToolResultBuffer(executionId);
        buffer.add(result);
        runtimeService.getObject().setVariableLocal(executionId, VAR_TOOL_RESULT_BUFFER, serialize(buffer));
    }

    public void appendAllToResultBuffer(String executionId, List<ToolResult> results) {
        List<ToolResult> buffer = loadToolResultBuffer(executionId);
        buffer.addAll(results);
        runtimeService.getObject().setVariableLocal(executionId, VAR_TOOL_RESULT_BUFFER, serialize(buffer));
    }

    public void clearToolResultBuffer(String executionId) {
        runtimeService.getObject().removeVariableLocal(executionId, VAR_TOOL_RESULT_BUFFER);
    }

    public List<ToolCallRequest> loadToolCallQueue(String executionId) {
        String json = (String) runtimeService.getObject().getVariableLocal(executionId, VAR_TOOL_CALL_QUEUE);
        if (json == null) {
            return new ArrayList<>();
        }
        return deserialize(json, QUEUE_TYPE);
    }

    public void saveToolCallQueue(String executionId, List<ToolCallRequest> queue) {
        runtimeService.getObject().setVariableLocal(executionId, VAR_TOOL_CALL_QUEUE, serialize(queue));
    }

    private Set<String> loadPendingToolCalls(String executionId) {
        String json = (String) runtimeService.getObject().getVariableLocal(executionId, VAR_PENDING_TOOL_CALLS);
        if (json == null) {
            return new HashSet<>();
        }
        return new HashSet<>(deserialize(json, PENDING_TYPE));
    }

    private String serialize(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize agent state", e);
        }
    }

    private <T> T deserialize(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize agent state", e);
        }
    }
}

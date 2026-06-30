package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model.ToolResult;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persists and retrieves per-execution agent state using process variables on the
 * scope execution.
 *
 * <p>Agent state is stored as JSON strings in the following local execution variables:
 * <ul>
 *   <li>{@code _agentConversationHistory} — the accumulated conversation turns</li>
 *   <li>{@code _agentPendingToolCalls} — the set of tool-call ids that have been
 *       dispatched but not yet completed</li>
 *   <li>{@code _agentToolResultBuffer} — tool results received since the last LLM
 *       call, held until all pending calls are complete</li>
 *   <li>{@code _agentToolCallQueue} — tool calls queued for sequential dispatch</li>
 * </ul>
 *
 * <p>The pending-set protocol works as follows: when tools are dispatched,
 * their call ids are saved via {@link #savePendingToolCalls}. As each tool
 * finishes, {@link #completeToolCall} removes it from the pending set and
 * returns {@code true} only when the set becomes empty, signalling that the
 * buffer can be flushed and the next step triggered.
 */
public class AgentStateManager {

    private static final Logger LOG = LoggerFactory.getLogger(AgentStateManager.class);

    private static final String VAR_CONVERSATION_HISTORY = "_agentConversationHistory";
    private static final String VAR_PENDING_TOOL_CALLS = "_agentPendingToolCalls";
    private static final String VAR_TOOL_RESULT_BUFFER = "_agentToolResultBuffer";
    private static final String VAR_TOOL_CALL_QUEUE = "_agentToolCallQueue";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<ConversationEntry>> HISTORY_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<Set<String>> PENDING_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ToolResult>> RESULT_BUFFER_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<ToolCallRequest>> QUEUE_TYPE = new TypeReference<>() {
    };

    public AgentStateManager() {
    }

    /**
     * Loads the conversation history for the given execution.
     *
     * @param runtimeService the runtime service used to read the execution's local variables
     * @param executionId    the scope execution id
     * @return the current conversation history; an empty list if none has been saved yet
     */
    public List<ConversationEntry> loadHistory(RuntimeService runtimeService, String executionId) {
        String json =
                (String) runtimeService.getVariableLocal(executionId, VAR_CONVERSATION_HISTORY);
        if (json == null) {
            return new ArrayList<>();
        }
        return deserialize(json, HISTORY_TYPE);
    }

    /**
     * Persists the conversation history for the given execution, replacing any
     * previously saved history.
     *
     * @param runtimeService the runtime service used to write the execution's local variables
     * @param executionId    the scope execution id
     * @param history        the history to save; must not be {@code null}
     */
    public void saveHistory(RuntimeService runtimeService, String executionId, List<ConversationEntry> history) {
        runtimeService.setVariableLocal(executionId, VAR_CONVERSATION_HISTORY, serialize(history));
    }

    /**
     * Saves the set of tool-call ids that have been dispatched and are awaiting
     * completion, replacing any previously saved set.
     *
     * @param runtimeService the runtime service used to write the execution's local variables
     * @param executionId the scope execution id
     * @param pending     the set of outstanding tool-call ids; must not be {@code null}
     */
    public void savePendingToolCalls(RuntimeService runtimeService, String executionId, Set<String> pending) {
        runtimeService.setVariableLocal(executionId, VAR_PENDING_TOOL_CALLS, serialize(pending));
    }

    /**
     * Returns {@code true} if {@code toolCallId} is in the pending set for the given
     * execution.
     *
     * @param runtimeService the runtime service used to read the execution's local variables
     * @param executionId the scope execution id
     * @param toolCallId  the tool-call id to check
     * @return {@code true} if the call is still pending, {@code false} otherwise
     */
    public boolean isPendingToolCall(RuntimeService runtimeService, String executionId, String toolCallId) {
        return loadPendingToolCalls(runtimeService, executionId).contains(toolCallId);
    }

    /**
     * Removes {@code toolCallId} from the pending set and persists the updated set.
     *
     * @param runtimeService the runtime service used to read and write the execution's local variables
     * @param executionId the scope execution id
     * @param toolCallId  the tool-call id that has completed
     * @return {@code true} if the pending set is now empty (i.e. all dispatched tools
     *         have completed), {@code false} if there are still outstanding calls
     */
    public boolean completeToolCall(RuntimeService runtimeService, String executionId, String toolCallId) {
        Set<String> pending = loadPendingToolCalls(runtimeService, executionId);
        LOG.debug("completeToolCall() scope='{}' removing='{}' pendingBefore={}",
                executionId, toolCallId, pending);
        pending.remove(toolCallId);
        savePendingToolCalls(runtimeService, executionId, pending);
        LOG.debug("completeToolCall() scope='{}' pendingAfter={} allCompleted={}",
                executionId, pending, pending.isEmpty());
        return pending.isEmpty();
    }

    /**
     * Loads all tool results accumulated in the buffer since it was last cleared.
     *
     * @param runtimeService the runtime service used to read the execution's local variables
     * @param executionId the scope execution id
     * @return the buffered results; an empty list if the buffer is empty
     */
    public List<ToolResult> loadToolResultBuffer(RuntimeService runtimeService, String executionId) {
        String json = (String) runtimeService.getVariableLocal(executionId, VAR_TOOL_RESULT_BUFFER);
        if (json == null) {
            return new ArrayList<>();
        }
        return deserialize(json, RESULT_BUFFER_TYPE);
    }

    /**
     * Appends a single tool result to the buffer.
     *
     * @param runtimeService the runtime service used to read and write the execution's local variables
     * @param executionId the scope execution id
     * @param result      the result to append; must not be {@code null}
     */
    public void appendToResultBuffer(RuntimeService runtimeService, String executionId, ToolResult result) {
        List<ToolResult> buffer = loadToolResultBuffer(runtimeService, executionId);
        buffer.add(result);
        runtimeService.setVariableLocal(executionId, VAR_TOOL_RESULT_BUFFER, serialize(buffer));
    }

    /**
     * Appends all of the given tool results to the buffer, preserving existing entries.
     *
     * @param runtimeService the runtime service used to read and write the execution's local variables
     * @param executionId the scope execution id
     * @param results     the results to append; must not be {@code null}
     */
    public void appendAllToResultBuffer(RuntimeService runtimeService, String executionId, List<ToolResult> results) {
        List<ToolResult> buffer = loadToolResultBuffer(runtimeService, executionId);
        buffer.addAll(results);
        runtimeService.setVariableLocal(executionId, VAR_TOOL_RESULT_BUFFER, serialize(buffer));
    }

    /**
     * Removes the tool result buffer variable from the execution, ready for the next
     * dispatch cycle.
     *
     * @param runtimeService the runtime service used to remove the execution's local variable
     * @param executionId the scope execution id
     */
    public void clearToolResultBuffer(RuntimeService runtimeService, String executionId) {
        runtimeService.removeVariableLocal(executionId, VAR_TOOL_RESULT_BUFFER);
    }

    /**
     * Loads the queued tool calls for the given execution.
     *
     * @param runtimeService the runtime service used to read the execution's local variables
     * @param executionId the scope execution id
     * @return the queued tool calls; an empty list if none have been saved
     */
    public List<ToolCallRequest> loadToolCallQueue(RuntimeService runtimeService, String executionId) {
        String json = (String) runtimeService.getVariableLocal(executionId, VAR_TOOL_CALL_QUEUE);
        if (json == null) {
            return new ArrayList<>();
        }
        return deserialize(json, QUEUE_TYPE);
    }

    /**
     * Saves the tool call queue for the given execution, replacing any previously
     * saved queue.
     *
     * @param runtimeService the runtime service used to write the execution's local variables
     * @param executionId the scope execution id
     * @param queue       the tool calls to queue; must not be {@code null}
     */
    public void saveToolCallQueue(RuntimeService runtimeService, String executionId, List<ToolCallRequest> queue) {
        runtimeService.setVariableLocal(executionId, VAR_TOOL_CALL_QUEUE, serialize(queue));
    }

    private Set<String> loadPendingToolCalls(RuntimeService runtimeService, String executionId) {
        String json = (String) runtimeService.getVariableLocal(executionId, VAR_PENDING_TOOL_CALLS);
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

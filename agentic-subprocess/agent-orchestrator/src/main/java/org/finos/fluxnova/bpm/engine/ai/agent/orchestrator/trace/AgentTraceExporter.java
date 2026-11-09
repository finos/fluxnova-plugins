package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.trace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.Role;
import org.finos.fluxnova.bpm.engine.shared.model.TokenUsage;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports agent conversation state and token usage as process variables for traceability.
 */
public final class AgentTraceExporter {

  private static final Logger LOG = LoggerFactory.getLogger(AgentTraceExporter.class);

  public static final String VAR_LAST_RESPONSE = "agentLastResponse";
  public static final String VAR_CONVERSATION_TRACE = "agentConversationTrace";
  public static final String VAR_CONVERSATION_HISTORY = "agentConversationHistory";
  public static final String VAR_PROMPT_TOKENS = "agentPromptTokens";
  public static final String VAR_COMPLETION_TOKENS = "agentCompletionTokens";
  public static final String VAR_TOTAL_TOKENS = "agentTotalTokens";
  public static final String VAR_TOKEN_USAGE = "agentTokenUsage";
  public static final String VAR_LLM_CALL_COUNT = "agentLlmCallCount";

  // Internal scope-local variable keys for accumulation across turns
  static final String INTERNAL_PROMPT_TOKENS = "_agentAccPromptTokens";
  static final String INTERNAL_COMPLETION_TOKENS = "_agentAccCompletionTokens";
  static final String INTERNAL_TOTAL_TOKENS = "_agentAccTotalTokens";
  static final String INTERNAL_LLM_CALL_COUNT = "_agentLlmCallCount";

  private AgentTraceExporter() {}

  public static Map<String, Object> exportTraceVariables(
      AgentStateManager stateManager, RuntimeService runtimeService, String scopeExecutionId) {
    List<ConversationEntry> history = stateManager.loadHistory(runtimeService, scopeExecutionId);
    if (history.isEmpty()) {
      return Map.of();
    }

    Map<String, Object> variables = new LinkedHashMap<>();
    String lastResponse = extractLastAssistantMessage(history);
    if (lastResponse != null && !lastResponse.isBlank()) {
      variables.put(VAR_LAST_RESPONSE, lastResponse);
    }

    String trace = formatTrace(history);
    if (!trace.isBlank()) {
      variables.put(VAR_CONVERSATION_TRACE, trace);
    }

    Object rawHistory =
        runtimeService.getVariableLocal(scopeExecutionId, "_agentConversationHistory");
    String historyJson = toStringValue(rawHistory);
    if (historyJson != null && !historyJson.isBlank()) {
      variables.put(VAR_CONVERSATION_HISTORY, historyJson);
    }

    // Token usage variables
    long promptTokens = loadLongVariable(runtimeService, scopeExecutionId, INTERNAL_PROMPT_TOKENS);
    long completionTokens = loadLongVariable(runtimeService, scopeExecutionId, INTERNAL_COMPLETION_TOKENS);
    long totalTokens = loadLongVariable(runtimeService, scopeExecutionId, INTERNAL_TOTAL_TOKENS);
    int llmCallCount = loadIntVariable(runtimeService, scopeExecutionId, INTERNAL_LLM_CALL_COUNT);

    if (llmCallCount > 0) {
      variables.put(VAR_PROMPT_TOKENS, promptTokens);
      variables.put(VAR_COMPLETION_TOKENS, completionTokens);
      variables.put(VAR_TOTAL_TOKENS, totalTokens);
      variables.put(VAR_LLM_CALL_COUNT, llmCallCount);
      variables.put(VAR_TOKEN_USAGE, String.format(
          "{\"promptTokens\":%d,\"completionTokens\":%d,\"totalTokens\":%d,\"llmCalls\":%d}",
          promptTokens, completionTokens, totalTokens, llmCallCount));
    }

    return variables;
  }

  /**
   * Accumulates token usage from a single LLM call into scope-local variables.
   * Called after each successful LLM call in the orchestration loop.
   *
   * @param runtimeService    the runtime service for variable access
   * @param scopeExecutionId  the ad-hoc subprocess scope execution id
   * @param tokenUsage        the token usage from this LLM call; may be {@code null} if the
   *                          provider does not report usage
   */
  public static void accumulateTokenUsage(
      RuntimeService runtimeService, String scopeExecutionId, TokenUsage tokenUsage) {
    // Always increment call count
    int callCount = loadIntVariable(runtimeService, scopeExecutionId, INTERNAL_LLM_CALL_COUNT) + 1;
    runtimeService.setVariableLocal(scopeExecutionId, INTERNAL_LLM_CALL_COUNT, callCount);

    if (tokenUsage == null) {
      LOG.warn("LLM provider did not return token usage metadata for scope '{}', skipping token accumulation",
          scopeExecutionId);
      return;
    }

    long promptTokens = loadLongVariable(runtimeService, scopeExecutionId, INTERNAL_PROMPT_TOKENS)
        + tokenUsage.promptTokens();
    long completionTokens = loadLongVariable(runtimeService, scopeExecutionId, INTERNAL_COMPLETION_TOKENS)
        + tokenUsage.completionTokens();
    long totalTokens = loadLongVariable(runtimeService, scopeExecutionId, INTERNAL_TOTAL_TOKENS)
        + tokenUsage.totalTokens();

    runtimeService.setVariableLocal(scopeExecutionId, INTERNAL_PROMPT_TOKENS, promptTokens);
    runtimeService.setVariableLocal(scopeExecutionId, INTERNAL_COMPLETION_TOKENS, completionTokens);
    runtimeService.setVariableLocal(scopeExecutionId, INTERNAL_TOTAL_TOKENS, totalTokens);

    LOG.debug("Token usage for scope '{}': prompt={}, completion={}, total={}, callCount={}",
        scopeExecutionId, promptTokens, completionTokens, totalTokens, callCount);
  }

  /**
   * Copies the current agent trace from the scope execution to the root process instance so it
   * is visible in Cockpit/Monitoring while the agent is still running.
   */
  public static void publishToProcessInstance(
      AgentStateManager stateManager,
      RuntimeService runtimeService,
      String scopeExecutionId,
      String processInstanceId) {
    Map<String, Object> traceVariables =
        exportTraceVariables(stateManager, runtimeService, scopeExecutionId);
    if (!traceVariables.isEmpty()) {
      runtimeService.setVariables(processInstanceId, traceVariables);
    }
  }

  static String extractLastAssistantMessage(List<ConversationEntry> history) {
    for (int index = history.size() - 1; index >= 0; index--) {
      ConversationEntry entry = history.get(index);
      if (entry.role() == Role.ASSISTANT
          && entry.content() != null
          && !entry.content().isBlank()) {
        return entry.content();
      }
    }
    return null;
  }

  static String formatTrace(List<ConversationEntry> history) {
    StringBuilder trace = new StringBuilder();
    for (ConversationEntry entry : history) {
      switch (entry.role()) {
        case ASSISTANT -> appendAssistantEntry(trace, entry);
        case TOOL -> appendToolEntry(trace, entry);
        case USER -> {
          if (entry.content() != null && !entry.content().isBlank()) {
            trace.append("User: ").append(entry.content()).append('\n');
          }
        }
        case SYSTEM -> {
          // omitted from human-readable trace
        }
      }
    }
    return trace.toString().trim();
  }

  private static void appendAssistantEntry(StringBuilder trace, ConversationEntry entry) {
    if (entry.content() != null && !entry.content().isBlank()) {
      trace.append("Assistant: ").append(entry.content()).append('\n');
    }
    if (entry.toolCalls() != null) {
      for (ToolCallRequest toolCall : entry.toolCalls()) {
        trace.append("  -> tool call: ").append(toolCall.toolId()).append('\n');
      }
    }
  }

  private static void appendToolEntry(StringBuilder trace, ConversationEntry entry) {
    trace.append("Tool result [").append(entry.toolCallId()).append("]: ");
    if (entry.toolResult() == null || entry.toolResult().isEmpty()) {
      trace.append("(empty)\n");
      return;
    }
    trace.append(entry.toolResult()).append('\n');
  }

  /**
   * Converts a process variable value to String, handling the case where the engine
   * stores large strings as {@code byte[]} (binary serialization).
   */
  private static String toStringValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String s) {
      return s;
    }
    if (value instanceof byte[] bytes) {
      return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
    return value.toString();
  }

  private static long loadLongVariable(RuntimeService runtimeService, String executionId, String name) {
    Object value = runtimeService.getVariableLocal(executionId, name);
    if (value == null) {
      return 0L;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    return 0L;
  }

  private static int loadIntVariable(RuntimeService runtimeService, String executionId, String name) {
    Object value = runtimeService.getVariableLocal(executionId, name);
    if (value == null) {
      return 0;
    }
    if (value instanceof Number n) {
      return n.intValue();
    }
    return 0;
  }
}

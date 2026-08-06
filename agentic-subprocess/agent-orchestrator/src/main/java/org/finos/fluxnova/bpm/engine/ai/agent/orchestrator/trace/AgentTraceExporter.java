package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.trace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.Role;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;

/**
 * Exports agent conversation state as process variables for traceability.
 */
public final class AgentTraceExporter {

  public static final String VAR_LAST_RESPONSE = "agentLastResponse";
  public static final String VAR_CONVERSATION_TRACE = "agentConversationTrace";
  public static final String VAR_CONVERSATION_HISTORY = "agentConversationHistory";

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

    String historyJson =
        (String) runtimeService.getVariableLocal(scopeExecutionId, "_agentConversationHistory");
    if (historyJson != null && !historyJson.isBlank()) {
      variables.put(VAR_CONVERSATION_HISTORY, historyJson);
    }

    return variables;
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
}

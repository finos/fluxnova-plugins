package org.finos.fluxnova.bpm.engine.ai.agent.llm.model;

import java.util.List;

public record LlmResponse(
    String assistantText,
    List<ToolCallRequest> toolCalls,
    List<ConversationEntry> updatedHistory
) {
    public LlmResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        updatedHistory = updatedHistory == null ? List.of() : List.copyOf(updatedHistory);
    }
}

package org.finos.fluxnova.bpm.engine.shared.model;

import java.util.List;

public record LlmResponse(String assistantText, List<ToolCallRequest> toolCalls,
        List<ConversationEntry> updatedHistory) {
}

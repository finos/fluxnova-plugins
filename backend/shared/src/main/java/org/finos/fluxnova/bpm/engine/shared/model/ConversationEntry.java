package org.finos.fluxnova.bpm.engine.shared.model;

import java.util.List;
import java.util.Map;

public record ConversationEntry(Role role, String content, List<ToolCallRequest> toolCalls,
        String toolCallId, Map<String, Object> toolResult) {
    public static ConversationEntry user(String content) {
        return new ConversationEntry(Role.USER, content, null, null, null);
    }

    public static ConversationEntry assistant(String content, List<ToolCallRequest> toolCalls) {
        return new ConversationEntry(Role.ASSISTANT, content, toolCalls, null, null);
    }

    public static ConversationEntry tool(String toolCallId, Map<String, Object> result) {
        return new ConversationEntry(Role.TOOL, null, null, toolCallId, result);
    }

    public static ConversationEntry system(String content) {
        return new ConversationEntry(Role.SYSTEM, content, null, null, null);
    }
}

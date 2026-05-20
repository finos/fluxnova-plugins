package org.finos.fluxnova.bpm.engine.ai.agent.llm.model;

import java.util.List;

/**
 * Provider-agnostic representation of a single conversation message.
 *
 * <p>Persistence of {@code List<ConversationEntry>} is the caller's responsibility.
 * Spring AI {@code Message} types are mapped to and from this DTO internally and do not
 * leak outside this module.</p>
 *
 * @param role          author of the message
 * @param content       message text; may be null when the assistant returned only tool calls
 *                      or when the entry is a tool result
 * @param toolCalls     tool calls requested by the assistant; empty for non-assistant entries
 * @param toolCallId    correlation id when {@code role} is {@link Role#TOOL}; otherwise null
 */
public record ConversationEntry(
    Role role,
    String content,
    List<ToolCallRequest> toolCalls,
    String toolCallId
) {
    public ConversationEntry {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static ConversationEntry user(String content) {
        return new ConversationEntry(Role.USER, content, List.of(), null);
    }

    public static ConversationEntry system(String content) {
        return new ConversationEntry(Role.SYSTEM, content, List.of(), null);
    }

    public static ConversationEntry assistant(String content, List<ToolCallRequest> toolCalls) {
        return new ConversationEntry(Role.ASSISTANT, content, toolCalls, null);
    }

    public static ConversationEntry toolResult(String toolCallId, String content) {
        return new ConversationEntry(Role.TOOL, content, List.of(), toolCallId);
    }
}

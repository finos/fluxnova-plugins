package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal helper for {@link SpringAiLlmService}. Maps between provider-agnostic
 * {@link ConversationEntry} DTOs and Spring AI {@link Message} types so Spring AI types
 * do not leak outside this package.
 */
class ConversationMapper {

    /**
     * Builds the Spring AI message list for the next ChatClient call.
     *
     * <p>Order: agent system prompt, prior conversation history, and — when a non-empty
     * context is supplied — a fresh system message containing the current variable
     * values as {@code name = value} lines. The context message is refreshed on
     * <strong>every</strong> turn so the LLM's view of available variables stays current
     * between iterations.</p>
     *
     * <p>The mapper does not introduce or label the context block — how the LLM should
     * interpret these variables is the agent author's responsibility (via
     * {@link AgentConfig#systemPrompt()}). When the context is {@code null} or empty, no
     * context message is emitted.</p>
     */
    List<Message> toSpringAi(AgentConfig config,
                             ResolvedContext context,
                             List<ConversationEntry> history) {
        List<Message> messages = new ArrayList<>();
        if (config.systemPrompt() != null && !config.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(config.systemPrompt()));
        }
        if (history != null) {
            for (ConversationEntry entry : history) {
                messages.add(entryToMessage(entry));
            }
        }
        String contextBlock = formatContext(context);
        if (contextBlock != null) {
            messages.add(new SystemMessage(contextBlock));
        }
        return messages;
    }

    private Message entryToMessage(ConversationEntry entry) {
        final String content = entry.content() == null ? "" : entry.content();
        return switch (entry.role()) {
            case SYSTEM -> new SystemMessage(content);
            case USER -> new UserMessage(content);
            case ASSISTANT -> AssistantMessage.builder()
                    .content(content)
                    .toolCalls(entry.toolCalls().stream()
                            .map(tc -> new AssistantMessage.ToolCall(
                                    tc.toolCallId(),
                                    "function",
                                    tc.toolId(),
                                    "{}"))
                            .collect(Collectors.toList()))
                    .build();
            case TOOL -> ToolResponseMessage.builder()
                    .responses(List.of(
                            new ToolResponseMessage.ToolResponse(
                                    entry.toolCallId(),
                                    "",
                                    !entry.toolResult().containsKey("error") ? entry.toolResult().values().toString() : "error")))
                    .build();
        };
    }

    /**
     * Extracts assistant text and tool calls from a Spring AI {@link ChatResponse} and
     * appends a corresponding {@link ConversationEntry} to the supplied prior history.
     */
    LlmResponse toLlmResponse(ChatResponse response, List<ConversationEntry> priorHistory) {
        AssistantMessage message = response.getResult().getOutput();

        String text = message.getText();
        List<AssistantMessage.ToolCall> springCalls = message.getToolCalls();

        List<ToolCallRequest> toolCalls = springCalls.stream()
                .map(tc -> new ToolCallRequest(tc.id(), tc.name()))
                .collect(Collectors.toList());

        List<ConversationEntry> updated = new ArrayList<>(
                priorHistory == null ? List.of() : priorHistory);
        updated.add(ConversationEntry.assistant(text, toolCalls));

        return new LlmResponse(text, toolCalls, updated);
    }

    private static String formatContext(ResolvedContext context) {
        if (context == null || context.variables() == null || context.variables().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        context.variables().forEach((name, value) -> {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(name).append(" = ").append(format(value));
        });
        return sb.toString();
    }

    private static String format(Object value) {
        if (value == null) return "null";
        return value.toString();
    }
}

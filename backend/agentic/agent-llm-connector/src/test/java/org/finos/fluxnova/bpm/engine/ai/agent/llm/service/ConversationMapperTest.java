package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.shared.model.ToolCallRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.finos.fluxnova.bpm.engine.shared.model.Role.ASSISTANT;

class ConversationMapperTest {

    private final ConversationMapper mapper = new ConversationMapper();

    private AgentConfig agentConfig(String systemPrompt) {
        return new AgentConfig("proc-1", "agent-1", "ollama", "llama3.1",
                systemPrompt, "agent-1");
    }

    @Test
    void toSpringAi_whenContextIsNonEmpty_emitsSystemPromptAndContextMessages() {
        AgentConfig config = agentConfig("You are a credit-check agent.");
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("customerId", "c-42");
        vars.put("applicationAmount", 10_000);
        ResolvedContext context = new ResolvedContext(vars);

        List<Message> messages = mapper.toSpringAi(config, context, List.of());

        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(messages.getFirst().getText()).isEqualTo("You are a credit-check agent.");
        assertThat(messages.get(1).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(messages.get(1).getText())
                .contains("customerId = c-42")
                .contains("applicationAmount = 10000");
    }

    @Test
    void toSpringAi_whenContextIsEmpty_emitsOnlySystemMessage() {
        AgentConfig config = agentConfig("sys");
        ResolvedContext context = new ResolvedContext(Map.of());

        List<Message> messages = mapper.toSpringAi(config, context, List.of());

        assertThat(messages)
                .as("empty context should not emit a context system message")
                .hasSize(1);
        assertThat(messages.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(messages.getFirst().getText()).isEqualTo("sys");
    }

    @Test
    void toSpringAi_whenContextIsNonEmpty_formatsVariablesAsKeyValueLinesWithoutPrologue() {
        AgentConfig config = agentConfig("sys");
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("customerId", "c-1");
        vars.put("amount", 250);

        List<Message> messages = mapper.toSpringAi(config, new ResolvedContext(vars), List.of());

        assertThat(messages).hasSize(2);
        assertThat(messages.get(1).getText())
                .as("context message should be raw key=value lines without prologue")
                .isEqualTo("customerId = c-1\namount = 250");
    }

    @Test
    void toSpringAi_whenHistoryContainsMixedRoles_mapsEachEntryToCorrectMessageType() {
        AgentConfig config = agentConfig("sys");
        ResolvedContext context = new ResolvedContext(Map.of());

        List<ConversationEntry> history = List.of(
                ConversationEntry.user("hello"),
                ConversationEntry.assistant("calling tool",
                        List.of(new ToolCallRequest("call-1", "creditScoreCheck"))),
                ConversationEntry.tool("call-1", Map.of("status", "score=720"))
        );

        List<Message> messages = mapper.toSpringAi(config, context, history);

        // [system, user, assistant, tool] — empty context emits no trailing message
        assertThat(messages).hasSize(4);
        assertThat(messages.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(messages.get(1).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(messages.get(2).getMessageType()).isEqualTo(MessageType.ASSISTANT);
        AssistantMessage assistant = (AssistantMessage) messages.get(2);
        assertThat(assistant.getToolCalls()).hasSize(1);
        assertThat(assistant.getToolCalls().getFirst().id()).isEqualTo("call-1");
        assertThat(assistant.getToolCalls().getFirst().name()).isEqualTo("creditScoreCheck");
        assertThat(messages.get(3).getMessageType()).isEqualTo(MessageType.TOOL);
        ToolResponseMessage toolResponse = (ToolResponseMessage) messages.get(3);
        assertThat(toolResponse.getResponses()).hasSize(1);
        assertThat(toolResponse.getResponses().getFirst().id()).isEqualTo("call-1");
        assertThat(toolResponse.getResponses().getFirst().responseData()).isEqualTo("[score=720]");
    }

    @Test
    void toLlmResponse_whenResponseContainsToolCalls_extractsTextAndCallsAndAppendsAssistantEntry() {
        List<ConversationEntry> prior = List.of(ConversationEntry.user("please run a check"));
        AssistantMessage assistant = AssistantMessage.builder()
                .content("Running credit check.")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "creditScoreCheck", "{}")))
                .build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));

        LlmResponse llm = mapper.toLlmResponse(response, prior);

        assertThat(llm.assistantText()).isEqualTo("Running credit check.");
        assertThat(llm.toolCalls()).hasSize(1);
        assertThat(llm.toolCalls().getFirst().toolCallId()).isEqualTo("call-1");
        assertThat(llm.toolCalls().getFirst().toolId()).isEqualTo("creditScoreCheck");
        assertThat(llm.updatedHistory()).hasSize(2);
        ConversationEntry appended = llm.updatedHistory().get(1);
        assertThat(appended.role()).isEqualTo(ASSISTANT);
        assertThat(appended.content()).isEqualTo("Running credit check.");
        assertThat(appended.toolCalls()).hasSize(1);
    }

    @Test
    void toLlmResponse_whenResponseHasNoToolCalls_returnsEmptyToolCallListAndAppendsAssistantEntry() {
        AssistantMessage assistant = new AssistantMessage("All done.");
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));

        LlmResponse llm = mapper.toLlmResponse(response, List.of());

        assertThat(llm.assistantText()).isEqualTo("All done.");
        assertThat(llm.toolCalls())
                .as("empty tool calls signals 'done' to the caller")
                .isEmpty();
        assertThat(llm.updatedHistory()).hasSize(1);
    }

    @Test
    void toSpringAi_whenContextIsNull_doesNotEmitContextMessage() {
        AgentConfig config = agentConfig("sys");

        List<Message> messages = mapper.toSpringAi(config, null,
                List.of(ConversationEntry.user("hi")));

        // [system, user] — no context system message tacked on the end
        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(messages.get(1).getMessageType()).isEqualTo(MessageType.USER);
    }
}

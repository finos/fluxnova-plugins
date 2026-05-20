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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.finos.fluxnova.bpm.engine.shared.model.Role.ASSISTANT;
import static org.junit.jupiter.api.Assertions.*;

class ConversationMapperTest {

    private final ConversationMapper mapper = new ConversationMapper();

    private AgentConfig agentConfig(String systemPrompt) {
        return new AgentConfig("proc-1", "agent-1", "ollama", "llama3.1",
            systemPrompt, "agent-1");
    }

    @Test
    void prependsSystemPromptAndAppendsContextOnEveryTurn() {
        AgentConfig config = agentConfig("You are a credit-check agent.");
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("customerId", "c-42");
        vars.put("applicationAmount", 10_000);
        ResolvedContext context = new ResolvedContext(vars);

        List<Message> messages = mapper.toSpringAi(config, context, List.of());

        assertEquals(2, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals("You are a credit-check agent.", messages.get(0).getText());
        assertEquals(MessageType.SYSTEM, messages.get(1).getMessageType());
        assertTrue(messages.get(1).getText().contains("customerId = c-42"));
        assertTrue(messages.get(1).getText().contains("applicationAmount = 10000"));
    }

    @Test
    void emptyContextEmitsNoMessageEvenWithSystemPrompt() {
        AgentConfig config = agentConfig("sys");
        ResolvedContext context = new ResolvedContext(Map.of());

        List<Message> messages = mapper.toSpringAi(config, context, List.of());

        assertEquals(1, messages.size(),
            "empty context should not emit a system message");
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals("sys", messages.get(0).getText());
    }

    @Test
    void contextMessageContainsOnlyKeyValueLinesWithNoPrologue() {
        AgentConfig config = agentConfig("sys");
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("customerId", "c-1");
        vars.put("amount", 250);

        List<Message> messages = mapper.toSpringAi(config, new ResolvedContext(vars), List.of());

        assertEquals(2, messages.size());
        String contextText = messages.get(1).getText();
        assertEquals("customerId = c-1\namount = 250", contextText,
            "context message should be raw key=value lines without prologue");
    }

    @Test
    void mapsEachConversationEntryByRole() {
        AgentConfig config = agentConfig("sys");
        ResolvedContext context = new ResolvedContext(Map.of());

        List<ConversationEntry> history = List.of(
            ConversationEntry.user("hello"),
            ConversationEntry.assistant("calling tool",
                List.of(new ToolCallRequest("call-1", "creditScoreCheck"))),
            ConversationEntry.tool("call-1", Map.of("status","score=720"))
        );

        List<Message> messages = mapper.toSpringAi(config, context, history);

        // [system, user, assistant, tool] — empty context emits no trailing message
        assertEquals(4, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals(MessageType.USER, messages.get(1).getMessageType());
        assertEquals(MessageType.ASSISTANT, messages.get(2).getMessageType());
        AssistantMessage assistant = (AssistantMessage) messages.get(2);
        assertEquals(1, assistant.getToolCalls().size());
        assertEquals("call-1", assistant.getToolCalls().get(0).id());
        assertEquals("creditScoreCheck", assistant.getToolCalls().get(0).name());
        assertEquals(MessageType.TOOL, messages.get(3).getMessageType());
        ToolResponseMessage toolResponse = (ToolResponseMessage) messages.get(3);
        assertEquals(1, toolResponse.getResponses().size());
        assertEquals("call-1", toolResponse.getResponses().get(0).id());
        assertEquals("[score=720]", toolResponse.getResponses().get(0).responseData());
    }

    @Test
    void toLlmResponseExtractsTextAndToolCallsAndAppendsAssistantEntry() {
        List<ConversationEntry> prior = List.of(ConversationEntry.user("please run a check"));
        AssistantMessage assistant = new AssistantMessage(
            "Running credit check.",
            Map.of(),
            List.of(new AssistantMessage.ToolCall("call-1", "function", "creditScoreCheck", "{}")));
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));

        LlmResponse llm = mapper.toLlmResponse(response, prior);

        assertEquals("Running credit check.", llm.assistantText());
        assertEquals(1, llm.toolCalls().size());
        assertEquals("call-1", llm.toolCalls().get(0).toolCallId());
        assertEquals("creditScoreCheck", llm.toolCalls().get(0).toolId());

        assertEquals(2, llm.updatedHistory().size());
        ConversationEntry appended = llm.updatedHistory().get(1);
        assertEquals(ASSISTANT, appended.role());
        assertEquals("Running credit check.", appended.content());
        assertEquals(1, appended.toolCalls().size());
    }

    @Test
    void toLlmResponseHandlesEmptyToolCalls() {
        AssistantMessage assistant = new AssistantMessage("All done.");
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));

        LlmResponse llm = mapper.toLlmResponse(response, List.of());

        assertEquals("All done.", llm.assistantText());
        assertTrue(llm.toolCalls().isEmpty(),
            "empty tool calls signals 'done' to the caller");
        assertEquals(1, llm.updatedHistory().size());
    }

    @Test
    void omitsContextMessageWhenContextIsNull() {
        AgentConfig config = agentConfig("sys");

        List<Message> messages = mapper.toSpringAi(config, null,
            List.of(ConversationEntry.user("hi")));

        // [system, user] — no context system message tacked on the end
        assertEquals(2, messages.size());
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals(MessageType.USER, messages.get(1).getMessageType());
    }

    @Test
    void unusedTypesReferencedInSwitchCompile() {
        // sanity: ensure UserMessage/SystemMessage are on the classpath at the imported names
        assertNotNull(new UserMessage("u"));
        assertNotNull(new SystemMessage("s"));
    }
}

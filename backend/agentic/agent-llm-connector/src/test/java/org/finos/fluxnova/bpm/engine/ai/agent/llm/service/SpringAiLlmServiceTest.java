package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.tool.AgentToolSchemaConverter;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SpringAiLlmServiceTest {

    private final AgentToolSchemaConverter converter = new AgentToolSchemaConverter();

    private AgentConfig config(String provider, String model) {
        return new AgentConfig("proc-1", "agent-1", provider, model,
            "You are an agent.", "agent-1");
    }

    private AgentToolCatalogue catalogue() {
        return new AgentToolCatalogue("proc-1", "agent-1", List.of(
            new AgentToolEntry("creditScoreCheck", "Credit Check",
                "Looks up the credit score.",
                Set.of("customerId"), Set.of("creditScore"))));
    }

    private ChatResponse stubResponse(String text, List<AssistantMessage.ToolCall> calls) {
        return new ChatResponse(List.of(
            new Generation(new AssistantMessage(text, Map.of(), calls))));
    }

    @Test
    void fullCallSelectsChatModelAndPassesPromptWithToolsAndContext() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
            .thenReturn(stubResponse("Working on it.", List.of(
                new AssistantMessage.ToolCall("call-1", "function", "creditScoreCheck", "{}"))));

        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", chatModel));
        LlmService service = new SpringAiLlmService(registry, converter);

        ResolvedContext context = new ResolvedContext(Map.of("customerId", "c-1"));
        LlmResponse response = service.call(
            config("ollama", "llama3.1"),
            catalogue(),
            context,
            List.of(ConversationEntry.user("Run a credit check")));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();

        List<Message> instructions = prompt.getInstructions();
        assertEquals(3, instructions.size());
        assertEquals(MessageType.SYSTEM, instructions.get(0).getMessageType());
        assertEquals("You are an agent.", instructions.get(0).getText());
        assertEquals(MessageType.USER, instructions.get(1).getMessageType());
        assertEquals("Run a credit check", instructions.get(1).getText());
        assertEquals(MessageType.SYSTEM, instructions.get(2).getMessageType());
        assertTrue(instructions.get(2).getText().contains("customerId = c-1"));

        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
        assertEquals("llama3.1", options.getModel());
        assertEquals(Boolean.FALSE, options.getInternalToolExecutionEnabled());

        assertEquals(1, options.getToolCallbacks().size());
        assertEquals("creditScoreCheck", options.getToolCallbacks().get(0).getToolDefinition().name());

        assertEquals("Working on it.", response.assistantText());
        assertEquals(1, response.toolCalls().size());
        assertEquals("creditScoreCheck", response.toolCalls().get(0).toolId());
        assertEquals(2, response.updatedHistory().size());
    }

    @Test
    void emptyToolCallListSignalsDone() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
            .thenReturn(stubResponse("All checks complete.", List.of()));

        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", chatModel));
        LlmService service = new SpringAiLlmService(registry, converter);

        LlmResponse response = service.call(
            config("ollama", "llama3.1"),
            catalogue(),
            new ResolvedContext(Map.of()),
            List.of());

        assertTrue(response.toolCalls().isEmpty());
        assertEquals("All checks complete.", response.assistantText());
    }

    @Test
    void unknownProviderRaisesError() {
        AgentProviderRegistry registry = new AgentProviderRegistry(Map::of);
        LlmService service = new SpringAiLlmService(registry, converter);

        assertThrows(IllegalStateException.class, () -> service.call(
            config("huggingface", "mistral-7b"), catalogue(), new ResolvedContext(Map.of()), List.of()));
    }

    @Test
    void configOnlyOverloadSendsSystemPromptWithNoToolsOrContext() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
            .thenReturn(stubResponse("Hello!", List.of()));

        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", chatModel));
        LlmService service = new SpringAiLlmService(registry, converter);

        LlmResponse response = service.call(config("ollama", "llama3.1"));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();

        List<Message> instructions = prompt.getInstructions();
        assertEquals(1, instructions.size(), "only the system prompt should be sent");
        assertEquals(MessageType.SYSTEM, instructions.get(0).getMessageType());
        assertEquals("You are an agent.", instructions.get(0).getText());

        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
        assertTrue(options.getToolCallbacks() == null || options.getToolCallbacks().isEmpty(),
            "no tool callbacks expected on the config-only overload");

        assertEquals("Hello!", response.assistantText());
        assertEquals(1, response.updatedHistory().size());
    }

    @Test
    void contextHistoryOverloadInjectsContextWithoutTools() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
            .thenReturn(stubResponse("Got it.", List.of()));

        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", chatModel));
        LlmService service = new SpringAiLlmService(registry, converter);

        ResolvedContext context = new ResolvedContext(Map.of("customerId", "c-7"));
        LlmResponse response = service.call(
            config("ollama", "llama3.1"),
            context,
            List.of(ConversationEntry.user("what's in scope?")));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        Prompt prompt = captor.getValue();

        // [system, user, context-system]
        List<Message> instructions = prompt.getInstructions();
        assertEquals(3, instructions.size());
        assertEquals(MessageType.SYSTEM, instructions.get(0).getMessageType());
        assertEquals(MessageType.USER, instructions.get(1).getMessageType());
        assertEquals(MessageType.SYSTEM, instructions.get(2).getMessageType());
        assertTrue(instructions.get(2).getText().contains("customerId = c-7"));

        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
        assertTrue(options.getToolCallbacks() == null || options.getToolCallbacks().isEmpty(),
            "context+history overload must advertise no tools");

        assertEquals("Got it.", response.assistantText());
    }

    @Test
    void historyOverloadSendsSystemPromptAndPriorHistoryWithoutContext() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
            .thenReturn(stubResponse("Continuing.", List.of()));

        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", chatModel));
        LlmService service = new SpringAiLlmService(registry, converter);

        LlmResponse response = service.call(
            config("ollama", "llama3.1"),
            List.of(ConversationEntry.user("hi"), ConversationEntry.assistant("hello", List.of())));

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        List<Message> instructions = captor.getValue().getInstructions();

        // [system prompt, user, assistant] — no trailing context message
        assertEquals(3, instructions.size());
        assertEquals(MessageType.SYSTEM, instructions.get(0).getMessageType());
        assertEquals(MessageType.USER, instructions.get(1).getMessageType());
        assertEquals(MessageType.ASSISTANT, instructions.get(2).getMessageType());

        assertEquals("Continuing.", response.assistantText());
        assertEquals(3, response.updatedHistory().size());
    }
}

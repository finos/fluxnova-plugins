package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderProperties;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistryConfig;
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
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                new Generation(AssistantMessage.builder().content(text).toolCalls(calls).build())));
    }

    @Test
    void call_whenCatalogueAndContextAndHistoryProvided_sendsToolsContextAndHistoryInPrompt() {
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
        assertThat(instructions).hasSize(3);
        assertThat(instructions.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(instructions.get(0).getText()).isEqualTo("You are an agent.");
        assertThat(instructions.get(1).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(instructions.get(1).getText()).isEqualTo("Run a credit check");
        assertThat(instructions.get(2).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(instructions.get(2).getText()).contains("customerId = c-1");

        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
        assertThat(options.getModel()).isEqualTo("llama3.1");
        assertThat(options.getInternalToolExecutionEnabled()).isEqualTo(Boolean.FALSE);
        assertThat(options.getToolCallbacks()).hasSize(1);
        assertThat(options.getToolCallbacks().get(0).getToolDefinition().name()).isEqualTo("creditScoreCheck");

        assertThat(response.assistantText()).isEqualTo("Working on it.");
        assertThat(response.toolCalls()).hasSize(1);
        assertThat(response.toolCalls().get(0).toolId()).isEqualTo("creditScoreCheck");
        assertThat(response.updatedHistory()).hasSize(2);
    }

    @Test
    void call_whenLlmReturnsNoToolCalls_responseHasEmptyToolCallList() {
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

        assertThat(response.toolCalls()).isEmpty();
        assertThat(response.assistantText()).isEqualTo("All checks complete.");
    }

    @Test
    void call_whenProviderNotRegistered_throwsIllegalStateException() {
        AgentProviderRegistry registry = new AgentProviderRegistry(Map::of);
        LlmService service = new SpringAiLlmService(registry, converter);

        assertThatThrownBy(() -> service.call(
                config("huggingface", "mistral-7b"), catalogue(), new ResolvedContext(Map.of()), List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void call_whenOnlyConfigProvided_sendsOnlySystemPromptWithNoToolsOrContext() {
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
        assertThat(instructions)
                .as("only the system prompt should be sent")
                .hasSize(1);
        assertThat(instructions.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(instructions.get(0).getText()).isEqualTo("You are an agent.");

        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
        assertThat(options.getToolCallbacks())
                .as("no tool callbacks expected on the config-only overload")
                .isNullOrEmpty();

        assertThat(response.assistantText()).isEqualTo("Hello!");
        assertThat(response.updatedHistory()).hasSize(1);
    }

    @Test
    void call_whenContextAndHistoryProvidedWithoutCatalogue_includesContextMessageWithoutTools() {
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
        assertThat(instructions).hasSize(3);
        assertThat(instructions.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(instructions.get(1).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(instructions.get(2).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(instructions.get(2).getText()).contains("customerId = c-7");

        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
        assertThat(options.getToolCallbacks())
                .as("context+history overload must advertise no tools")
                .isNullOrEmpty();

        assertThat(response.assistantText()).isEqualTo("Got it.");
    }

    @Test
    void call_whenProviderRegisteredViaOverride_routesCallToCustomChatModel() {
        ChatModel customModel = mock(ChatModel.class);
        when(customModel.call(any(Prompt.class)))
                .thenReturn(stubResponse("Done.", List.of()));

        // Simulates: fluxnova.ai.agent.provider-overrides.my-model=myCustomChatModelBean
        ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);
        when(beanFactory.getBeansOfType(ChatModel.class))
                .thenReturn(Map.of("myCustomChatModelBean", customModel));
        AgentProviderRegistry registry = AgentProviderRegistryConfig.build(
                beanFactory, propertiesWithOverride("my-model", "myCustomChatModelBean"));
        LlmService service = new SpringAiLlmService(registry, converter);

        LlmResponse response = service.call(config("my-model", "custom-v1"));

        verify(customModel).call(any(Prompt.class));
        assertThat(response.assistantText()).isEqualTo("Done.");
    }

    @Test
    void call_whenOnlyHistoryProvided_sendsSystemPromptAndHistoryWithoutContextMessage() {
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
        assertThat(instructions).hasSize(3);
        assertThat(instructions.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(instructions.get(1).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(instructions.get(2).getMessageType()).isEqualTo(MessageType.ASSISTANT);

        assertThat(response.assistantText()).isEqualTo("Continuing.");
        assertThat(response.updatedHistory()).hasSize(3);
    }

    @Test
    void call_whenLlmRequestsNonExistingTool_throwIllegalStateException() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(stubResponse("I need to check the credit score.", List.of(
                        new AssistantMessage.ToolCall("call-x", "function", "hallucinatedTool", "{}"))));
        ;

        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", chatModel));
        LlmService service = new SpringAiLlmService(registry, converter);

        assertThatThrownBy(() -> service.call(
                config("ollama", "llama3.1"),
                catalogue(), new ResolvedContext(Map.of()), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LLM called tool 'hallucinatedTool' which is not in the provided catalogue");
    }

    @Test
    void call_whenLlmRequestsToolWhenNoCatalogueProvided_throwIllegalStateException() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(stubResponse("I need to check the credit score.", List.of(
                        new AssistantMessage.ToolCall("call-x", "function", "hallucinatedTool", "{}"))));
        ;

        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", chatModel));
        LlmService service = new SpringAiLlmService(registry, converter);

        assertThatThrownBy(() -> service.call(
                config("ollama", "llama3.1"),
                new ResolvedContext(Map.of()), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LLM made tool calls but no tools were provided in the request");
    }


    private static AgentProviderProperties propertiesWithOverride(String providerId, String beanName) {
        AgentProviderProperties props = new AgentProviderProperties();
        props.setProviderOverrides(Map.of(providerId, beanName));
        return props;
    }
}

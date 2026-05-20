package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.model.LlmResponse;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.tool.AgentToolSchemaConverter;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Spring AI implementation of {@link LlmService}.
 *
 * <p>Uses {@link ChatClient} per call so that provider, model, and tool advertisement are
 * all derived from the supplied {@link AgentConfig}/catalogue. Internal tool execution is
 * disabled — the LLM's tool-call decision is returned to the caller unmodified.</p>
 */
public class SpringAiLlmService implements LlmService {

    private final AgentProviderRegistry providerRegistry;
    private final AgentToolSchemaConverter toolSchemaConverter;
    private final ConversationMapper conversationMapper = new ConversationMapper();

    public SpringAiLlmService(AgentProviderRegistry providerRegistry,
                              AgentToolSchemaConverter toolSchemaConverter) {
        this.providerRegistry = providerRegistry;
        this.toolSchemaConverter = toolSchemaConverter;
    }

    @Override
    public LlmResponse call(AgentConfig agentConfig) {
        return call(agentConfig, null, null, List.of());
    }

    @Override
    public LlmResponse call(AgentConfig agentConfig, List<ConversationEntry> conversationHistory) {
        return call(agentConfig, null, null, conversationHistory);
    }

    @Override
    public LlmResponse call(AgentConfig agentConfig,
                            ResolvedContext context,
                            List<ConversationEntry> conversationHistory) {
        return call(agentConfig, null, context, conversationHistory);
    }

    @Override
    public LlmResponse call(AgentConfig agentConfig,
                            AgentToolCatalogue catalogue,
                            ResolvedContext context,
                            List<ConversationEntry> conversationHistory) {

        ChatModel chatModel = providerRegistry.get(agentConfig.provider());

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
            .model(agentConfig.model())
            .internalToolExecutionEnabled(false)
            .build();

        ChatClient client = ChatClient.builder(chatModel)
            .defaultOptions(options)
            .build();

        List<ConversationEntry> history = conversationHistory == null ? List.of() : conversationHistory;
        List<ToolCallback> toolCallbacks = catalogue == null ? List.of() : toolSchemaConverter.convert(catalogue);
        List<Message> messages = conversationMapper.toSpringAi(agentConfig, context, history);

        ChatClient.ChatClientRequestSpec spec = client.prompt().messages(messages);
        if (!toolCallbacks.isEmpty()) {
            spec = spec.toolCallbacks(toolCallbacks);
        }

        ChatResponse response = spec.call().chatResponse();

        return conversationMapper.toLlmResponse(response, history);
    }
}

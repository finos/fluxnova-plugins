package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.tool.AgentToolSchemaConverter;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        List<ConversationEntry> history = conversationHistory == null ? List.of() : conversationHistory;
        List<ToolCallback> toolCallbacks = catalogue == null ? List.of() : toolSchemaConverter.convert(catalogue);
        List<Message> messages = conversationMapper.toSpringAi(agentConfig, context, history);

        // Build a ChatClient whose tool advisor never executes tools internally: the eligibility
        // checker always returns false, so the model's tool-call response is returned to us and the
        // orchestrator dispatches each call as a BPMN activity. We go through the ChatClient (rather
        // than calling the ChatModel directly) so Spring AI merges our options with the provider's
        // defaults. Spring AI 2.0 removed the per-request internalToolExecutionEnabled flag, so a
        // non-executing ToolCallingAdvisor is how we opt out. The ToolCallingManager is required but
        // never invoked (eligibility is always false).
        ToolCallingAdvisor nonExecutingToolAdvisor = ToolCallingAdvisor.builder()
                .toolCallingManager(ToolCallingManager.builder().build())
                .toolExecutionEligibilityChecker(response -> false)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(nonExecutingToolAdvisor)
                .build();

        ChatClient.ChatClientRequestSpec spec = client.prompt().messages(messages);
        if (!toolCallbacks.isEmpty()) {
            spec = spec.toolCallbacks(toolCallbacks);
        }

        ChatResponse springResponse = spec.call().chatResponse();
        LlmResponse response = conversationMapper.toLlmResponse(springResponse, history);

        if (springResponse.hasToolCalls()) {

            if (toolCallbacks.isEmpty()) {
                throw new IllegalStateException("LLM made tool calls but no tools were provided in the request");
            }

            Set<String> availableTools = catalogue.tools().stream()
                    .map(AgentToolEntry::elementId)
                    .collect(Collectors.toSet());

            response.toolCalls().forEach(toolCall -> {
                if (!availableTools.contains(toolCall.toolId())) {
                    throw new IllegalStateException("LLM called tool '" + toolCall.toolId() + "' which is not in the provided catalogue");
                }
            });


        }

        return response;
    }
}

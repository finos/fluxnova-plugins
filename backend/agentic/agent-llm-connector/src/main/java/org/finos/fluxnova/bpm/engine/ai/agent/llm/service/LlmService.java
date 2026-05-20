package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.model.ConversationEntry;
import org.finos.fluxnova.bpm.engine.shared.model.LlmResponse;

import java.util.List;

/**
 * Stateless façade for invoking an LLM. The caller supplies an {@link AgentConfig}
 * (which selects the provider, model, and system prompt) and optionally a tool catalogue,
 * resolved variable context, and prior conversation history. Each call returns the
 * assistant text, any tool-call requests the LLM made, and the updated history.
 *
 * <p>Persistence of the returned history is the caller's responsibility — this service
 * holds no state between calls.</p>
 */
public interface LlmService {

    /**
     * Single-shot call with no tools, no context, and no prior history.
     */
    LlmResponse call(AgentConfig agentConfig);

    /**
     * Continuation of an existing conversation with no tools and no context.
     */
    LlmResponse call(AgentConfig agentConfig, List<ConversationEntry> conversationHistory);

    /**
     * Continuation of an existing conversation with a context snapshot but no tools.
     * Suitable for context-aware Q&amp;A without tool invocation.
     */
    LlmResponse call(AgentConfig agentConfig,
                     ResolvedContext context,
                     List<ConversationEntry> conversationHistory);

    /**
     * Full agentic call: the LLM is given a tool catalogue to choose from, a snapshot of
     * the variables visible on this turn, and the conversation history so far.
     */
    LlmResponse call(AgentConfig agentConfig,
                     AgentToolCatalogue catalogue,
                     ResolvedContext context,
                     List<ConversationEntry> conversationHistory);
}

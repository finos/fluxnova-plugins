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
     *
     * @param agentConfig configuration selecting the provider, model, and system prompt
     * @return the LLM response including assistant text and an updated conversation history
     */
    LlmResponse call(AgentConfig agentConfig);

    /**
     * Continuation of an existing conversation with no tools and no context.
     *
     * @param agentConfig         configuration selecting the provider, model, and system prompt
     * @param conversationHistory prior turns in the conversation; may be empty but not
     *                            {@code null}
     * @return the LLM response including assistant text and an updated conversation history
     */
    LlmResponse call(AgentConfig agentConfig, List<ConversationEntry> conversationHistory);

    /**
     * Continuation of an existing conversation with a context snapshot but no tools.
     *
     * @param agentConfig         configuration selecting the provider, model, and system prompt
     * @param context             a snapshot of variables to inject into the conversation;
     *                            {@code null} or an empty variable map results in no context
     *                            being appended
     * @param conversationHistory prior turns in the conversation; may be empty but not
     *                            {@code null}
     * @return the LLM response including assistant text and an updated conversation history
     */
    LlmResponse call(AgentConfig agentConfig,
                     ResolvedContext context,
                     List<ConversationEntry> conversationHistory);

    /**
     * Full call with a tool catalogue, a context snapshot, and conversation history.
     *
     * <p>The LLM is advertised the tools in {@code catalogue} and may request one or more
     * tool calls. Tool calls are <em>not</em> executed by this service — they are returned
     * in {@link LlmResponse#toolCalls()} for the caller to dispatch. The caller is
     * responsible for persisting {@link LlmResponse#updatedHistory()} and passing it back
     * on the next call.
     *
     * @param agentConfig         configuration selecting the provider, model, and system prompt
     * @param catalogue           the tools to advertise to the LLM; {@code null} is treated
     *                            as an empty catalogue
     * @param context             a snapshot of variables to inject into the conversation;
     *                            {@code null} or an empty variable map results in no context
     *                            being appended
     * @param conversationHistory prior turns in the conversation; may be empty but not
     *                            {@code null}
     * @return the LLM response including assistant text, any tool-call requests the LLM
     *         made, and the updated conversation history
     */
    LlmResponse call(AgentConfig agentConfig,
                     AgentToolCatalogue catalogue,
                     ResolvedContext context,
                     List<ConversationEntry> conversationHistory);
}

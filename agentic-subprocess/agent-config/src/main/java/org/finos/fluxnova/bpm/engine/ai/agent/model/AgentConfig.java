package org.finos.fluxnova.bpm.engine.ai.agent.model;

/**
 * Immutable agent configuration extracted from a BPMN element.
 *
 * @param processDefinitionId process definition that owns the configured element
 * @param elementId           BPMN element id carrying the agent configuration
 * @param provider            AI provider identifier (e.g. {@code "openai"}, {@code "anthropic"}),
 *                            or a process-engine expression such as {@code "${llmProvider}"}
 *                            evaluated per instance at orchestration time
 * @param model               model identifier within the provider (e.g. {@code "gpt-5.4"}),
 *                            or a process-engine expression such as {@code "${llmModel}"}
 * @param systemPrompt        system prompt text supplied to the agent, may be {@code null};
 *                            may contain expressions (including composite text)
 * @param toolScopeElementId  BPMN element id that defines the tool-resolution scope; when omitted
 *                            in BPMN, this defaults to {@code elementId}
 * @param cacheStrategy       optional Anthropic prompt-cache strategy (e.g. {@code SYSTEM_AND_TOOLS}),
 *                            or an expression such as {@code "${cacheStrategy}"}; ignored for
 *                            non-Anthropic providers
 * @param promptCacheKey      optional OpenAI {@code prompt_cache_key}, or an expression such as
 *                            {@code "${promptCacheKey}"}; ignored for non-OpenAI providers
 */
public record AgentConfig(
    String processDefinitionId,
    String elementId,
    String provider,
    String model,
    String systemPrompt,
    String toolScopeElementId,
    String cacheStrategy,
    String promptCacheKey
) {

    /** Convenience constructor for configs without cache settings. */
    public AgentConfig(
            String processDefinitionId,
            String elementId,
            String provider,
            String model,
            String systemPrompt,
            String toolScopeElementId) {
        this(
                processDefinitionId,
                elementId,
                provider,
                model,
                systemPrompt,
                toolScopeElementId,
                null,
                null);
    }
}

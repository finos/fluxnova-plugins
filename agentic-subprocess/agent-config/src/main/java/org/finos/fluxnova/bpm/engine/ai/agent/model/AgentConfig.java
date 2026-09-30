package org.finos.fluxnova.bpm.engine.ai.agent.model;

/**
 * Immutable agent configuration extracted from a BPMN element.
 *
 * <p>{@code userPrompt} holds the <em>raw</em> attribute text as written in the BPMN model.
 * It may contain {@code ${...}} expressions, which are evaluated per process instance at
 * runtime by the orchestrator — never at parse time, because this record is cached per
 * process definition and shared by all instances. {@code systemPrompt} is used as literal text.
 *
 * @param processDefinitionId process definition that owns the configured element
 * @param elementId           BPMN element id carrying the agent configuration
 * @param provider            AI provider identifier (e.g. {@code "ollama"}, {@code "huggingface"})
 * @param model               model identifier within the provider (e.g. {@code "llama3"}, {@code "mistral"})
 * @param systemPrompt        system prompt text supplied to the agent, may be {@code null}
 * @param toolScopeElementId  BPMN element id that defines the tool-resolution scope; when omitted
 *                            in BPMN, this defaults to {@code elementId}
 * @param userPrompt          initial user message template sent on the agent's first turn, may be
 *                            {@code null}; when absent or resolving to blank, the orchestrator's
 *                            default initial message is used
 */
public record AgentConfig(
    String processDefinitionId,
    String elementId,
    String provider,
    String model,
    String systemPrompt,
    String toolScopeElementId,
    String userPrompt
) {

    /**
     * Backward-compatible constructor for configurations without a {@code userPrompt}.
     */
    public AgentConfig(String processDefinitionId, String elementId, String provider,
                       String model, String systemPrompt, String toolScopeElementId) {
        this(processDefinitionId, elementId, provider, model, systemPrompt, toolScopeElementId, null);
    }
}

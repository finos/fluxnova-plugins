package org.finos.fluxnova.bpm.engine.ai.agent.model;

/**
 * Immutable agent configuration extracted from a BPMN element.
 *
 * @param processDefinitionId process definition that owns the configured element
 * @param elementId           BPMN element id carrying the agent configuration
 * @param provider            AI provider identifier (e.g. {@code "ollama"}, {@code "huggingface"})
 * @param model               model identifier within the provider (e.g. {@code "llama3"}, {@code "mistral"})
 * @param systemPrompt        system prompt text supplied to the agent, may be {@code null}
 * @param toolScopeElementId  BPMN element id that defines the tool-resolution scope; when omitted
 *                            in BPMN, this defaults to {@code elementId}
 */
public record AgentConfig(
    String processDefinitionId,
    String elementId,
    String provider,
    String model,
    String systemPrompt,
    String toolScopeElementId
) {}

package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;

/**
 * Job handler configuration carrying the context for a single orchestration step.
 *
 * <p>Two distinct step types are represented by this configuration:
 * <ul>
 *   <li><b>Entry</b> — a new orchestration turn triggered on scope entry. Created via
 *       {@link #forEntry()}; {@code toolResult} is {@code null}.</li>
 *   <li><b>Tool completion</b> — a step triggered when a dispatched tool activity
 *       finishes. Created via {@link #forToolCompletion(ToolResult)}; {@code toolResult}
 *       carries the outcome of the completed tool.</li>
 * </ul>
 *
 * <p>The configuration is serialised to and from JSON for persistence as a job handler
 * configuration string.
 *
 * @param toolResult the completed tool result for a tool-completion step; {@code null}
 *                   for an entry step
 */
public record AgentOrchestrationConfig(
        ToolResult toolResult
) implements JobHandlerConfiguration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AgentOrchestrationConfig forEntry() {
        return new AgentOrchestrationConfig(null);
    }

    public static AgentOrchestrationConfig forToolCompletion(ToolResult result) {
        return new AgentOrchestrationConfig(result);
    }

    public boolean hasToolResult() {
        return toolResult != null;
    }

    @Override
    public String toCanonicalString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AgentOrchestrationConfig", e);
        }
    }

    public static AgentOrchestrationConfig fromCanonicalString(String canonicalString) {
        try {
            return MAPPER.readValue(canonicalString, AgentOrchestrationConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize AgentOrchestrationConfig", e);
        }
    }
}

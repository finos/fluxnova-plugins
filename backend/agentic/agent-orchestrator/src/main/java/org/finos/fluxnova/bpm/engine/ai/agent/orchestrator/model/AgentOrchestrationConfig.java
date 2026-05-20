package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;

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

package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ToolResult(
    String toolCallId,
    String toolElementId,
    String errorMessage
) {
    public static ToolResult error(String toolCallId, String message) {
        return new ToolResult(toolCallId, null, message);
    }

    @JsonIgnore
    public boolean isError() {
        return errorMessage != null;
    }
}

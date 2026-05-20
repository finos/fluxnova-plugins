package org.finos.fluxnova.bpm.engine.shared.model;

public record ToolCallRequest(
        String toolCallId,
        String toolId
) {
}

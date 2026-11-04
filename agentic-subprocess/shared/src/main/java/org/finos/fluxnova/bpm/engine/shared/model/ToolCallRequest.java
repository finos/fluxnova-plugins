package org.finos.fluxnova.bpm.engine.shared.model;

public record ToolCallRequest(String toolCallId, String toolId, String arguments) {
    /** Backward-compatible constructor for existing call sites. */
    public ToolCallRequest(String toolCallId, String toolId) {
        this(toolCallId, toolId, null);
    }
}

package org.finos.fluxnova.bpm.engine.shared.model;

public record ToolInvocationResult(
        String toolCallId,
        boolean success,
        String errorMessage
) {
    public static ToolInvocationResult success(String toolCallId) {
        return new ToolInvocationResult(toolCallId, true, null);
    }

    public static ToolInvocationResult failure(String toolCallId, String errorMessage) {
        return new ToolInvocationResult(toolCallId, false, errorMessage);
    }
}

package org.finos.fluxnova.bpm.engine.ai.agent.llm.model;

public record ToolCallRequest(
    String toolCallId,
    String toolId
) {}

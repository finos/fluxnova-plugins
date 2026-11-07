package org.finos.fluxnova.bpm.engine.shared.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ToolCallRequest(String toolCallId, String toolId, Map<String, Object> arguments) {

    public ToolCallRequest(String toolCallId, String toolId) {
        this(toolCallId, toolId, Map.of());
    }

    public ToolCallRequest {
        arguments = arguments == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }
}

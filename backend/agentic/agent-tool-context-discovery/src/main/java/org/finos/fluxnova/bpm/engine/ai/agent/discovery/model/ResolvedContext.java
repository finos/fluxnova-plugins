package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import java.util.Map;

public record ResolvedContext(
    Map<String, Object> variables
) {}

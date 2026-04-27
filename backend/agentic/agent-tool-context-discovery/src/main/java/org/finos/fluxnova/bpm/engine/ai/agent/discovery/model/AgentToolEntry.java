package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import java.util.Set;

public record AgentToolEntry(
    String elementId,
    String name,
    String description,
    Set<String> reads,
    Set<String> writes
) {}

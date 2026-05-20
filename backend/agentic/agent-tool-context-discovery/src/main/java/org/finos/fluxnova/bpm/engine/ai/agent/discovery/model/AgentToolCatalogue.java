package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import java.util.List;
import java.util.Optional;

public record AgentToolCatalogue(
    String processDefinitionId,
    String elementId,
    List<AgentToolEntry> tools
) {
    public Optional<AgentToolEntry> findById(String elementId) {
        return tools.stream()
                .filter(t -> t.elementId().equals(elementId))
                .findFirst();
    }
}

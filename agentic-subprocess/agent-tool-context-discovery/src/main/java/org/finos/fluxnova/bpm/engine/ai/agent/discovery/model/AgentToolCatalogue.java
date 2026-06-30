package org.finos.fluxnova.bpm.engine.ai.agent.discovery.model;

import java.util.List;
import java.util.Optional;

/**
 * An immutable catalogue of tools available within a BPMN scope.
 *
 * @param processDefinitionId the process definition that owns the scope
 * @param elementId           the id of the BPMN scope element this catalogue was built from
 * @param tools               the tools available within the scope; never {@code null},
 *                            but may be empty
 */
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

package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.repository.DecisionRequirementsDefinition;

/**
 * Result DTO for decision requirements definition query results.
 * Maps the fields from the engine's {@link DecisionRequirementsDefinition} interface.
 */
public record DecisionRequirementsDefinitionResultDto(
        String id,
        String key,
        String category,
        String name,
        int version,
        String resourceName,
        String deploymentId,
        String diagramResourceName,
        String tenantId,
        Integer historyTimeToLive
) {
    public static DecisionRequirementsDefinitionResultDto fromDecisionRequirementsDefinition(
            DecisionRequirementsDefinition decisionRequirementsDefinition) {
        return new DecisionRequirementsDefinitionResultDto(
                decisionRequirementsDefinition.getId(),
                decisionRequirementsDefinition.getKey(),
                decisionRequirementsDefinition.getCategory(),
                decisionRequirementsDefinition.getName(),
                decisionRequirementsDefinition.getVersion(),
                decisionRequirementsDefinition.getResourceName(),
                decisionRequirementsDefinition.getDeploymentId(),
                decisionRequirementsDefinition.getDiagramResourceName(),
                decisionRequirementsDefinition.getTenantId(),
                decisionRequirementsDefinition.getHistoryTimeToLive()
        );
    }
}

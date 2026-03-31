package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.repository.DecisionDefinition;

/**
 * Result DTO for decision definition query results.
 * Maps the fields from the engine's {@link DecisionDefinition} interface.
 */
public record DecisionDefinitionResultDto(
        String id,
        String key,
        String category,
        String name,
        int version,
        String resourceName,
        String deploymentId,
        String diagramResourceName,
        String tenantId,
        String decisionRequirementsDefinitionId,
        String decisionRequirementsDefinitionKey,
        String versionTag,
        Integer historyTimeToLive
) {
    public static DecisionDefinitionResultDto fromDecisionDefinition(DecisionDefinition decisionDefinition) {
        return new DecisionDefinitionResultDto(
                decisionDefinition.getId(),
                decisionDefinition.getKey(),
                decisionDefinition.getCategory(),
                decisionDefinition.getName(),
                decisionDefinition.getVersion(),
                decisionDefinition.getResourceName(),
                decisionDefinition.getDeploymentId(),
                decisionDefinition.getDiagramResourceName(),
                decisionDefinition.getTenantId(),
                decisionDefinition.getDecisionRequirementsDefinitionId(),
                decisionDefinition.getDecisionRequirementsDefinitionKey(),
                decisionDefinition.getVersionTag(),
                decisionDefinition.getHistoryTimeToLive()
        );
    }
}

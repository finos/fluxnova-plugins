package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.repository.CaseDefinition;

/**
 * Result DTO for case definition query results.
 * Maps the fields from the engine's {@link CaseDefinition} interface.
 */
public record CaseDefinitionResultDto(
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
    public static CaseDefinitionResultDto fromCaseDefinition(CaseDefinition caseDefinition) {
        return new CaseDefinitionResultDto(
                caseDefinition.getId(),
                caseDefinition.getKey(),
                caseDefinition.getCategory(),
                caseDefinition.getName(),
                caseDefinition.getVersion(),
                caseDefinition.getResourceName(),
                caseDefinition.getDeploymentId(),
                caseDefinition.getDiagramResourceName(),
                caseDefinition.getTenantId(),
                caseDefinition.getHistoryTimeToLive()
        );
    }
}

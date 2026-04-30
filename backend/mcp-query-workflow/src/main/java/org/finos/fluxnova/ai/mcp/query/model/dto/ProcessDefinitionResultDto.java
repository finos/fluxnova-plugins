package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;

/**
 * Result DTO for process definition query results.
 * Maps the fields from the engine's {@link ProcessDefinition} interface.
 */
public record ProcessDefinitionResultDto(
        String id,
        String key,
        String category,
        String description,
        String name,
        int version,
        String resourceName,
        String deploymentId,
        String diagramResourceName,
        boolean suspended,
        String tenantId,
        String versionTag,
        Integer historyTimeToLive,
        boolean startableInTasklist
) {
    public static ProcessDefinitionResultDto fromProcessDefinition(ProcessDefinition processDefinition) {
        return new ProcessDefinitionResultDto(
                processDefinition.getId(),
                processDefinition.getKey(),
                processDefinition.getCategory(),
                processDefinition.getDescription(),
                processDefinition.getName(),
                processDefinition.getVersion(),
                processDefinition.getResourceName(),
                processDefinition.getDeploymentId(),
                processDefinition.getDiagramResourceName(),
                processDefinition.isSuspended(),
                processDefinition.getTenantId(),
                processDefinition.getVersionTag(),
                processDefinition.getHistoryTimeToLive(),
                processDefinition.isStartableInTasklist()
        );
    }
}

package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.repository.Deployment;

import java.util.Date;

/**
 * Result DTO for deployment query results.
 * Maps the fields from the engine's {@link Deployment} interface.
 */
public record DeploymentResultDto(
        String id,
        String name,
        Date deploymentTime,
        String source,
        String tenantId
) {
    public static DeploymentResultDto fromDeployment(Deployment deployment) {
        return new DeploymentResultDto(
                deployment.getId(),
                deployment.getName(),
                deployment.getDeploymentTime(),
                deployment.getSource(),
                deployment.getTenantId()
        );
    }
}

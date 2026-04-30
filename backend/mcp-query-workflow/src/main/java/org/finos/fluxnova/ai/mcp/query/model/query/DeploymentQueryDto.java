package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.DeploymentQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying deployments via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering deployments.")
public record DeploymentQueryDto(
        @Schema(description = "Filter by deployment id.")
        String deploymentId,

        @Schema(description = "Filter by the deployment name. Exact match.")
        String name,

        @Schema(description = "Filter by deployment names that the parameter is a substring of. "
                + "The parameter may include the wildcard character '%'.")
        String nameLike,

        @Schema(description = "Filter by the deployment source.")
        String source,

        @Schema(description = "Only include deployments that have no source. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withoutSource,

        @Schema(description = "Restricts to all deployments after the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date after,

        @Schema(description = "Restricts to all deployments before the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date before,

        @Schema(description = "Filter by a list of tenant ids. A deployment must have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include deployments which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Include deployments which belong to no tenant. "
                + "Can be used in combination with tenantIdIn.")
        Boolean includeDeploymentsWithoutTenantId
) {
    /**
     * Create a new DeploymentQuery from the RepositoryService, with all non-null filter criteria applied.
     */
    public DeploymentQuery toQuery(RepositoryService repositoryService) {
        DeploymentQuery query = repositoryService.createDeploymentQuery();
        if (deploymentId != null) {
            query.deploymentId(deploymentId);
        }
        if (name != null) {
            query.deploymentName(name);
        }
        if (nameLike != null) {
            query.deploymentNameLike(nameLike);
        }
        if (source != null) {
            query.deploymentSource(source);
        }
        if (Boolean.TRUE.equals(withoutSource)) {
            query.deploymentSource(null);
        }
        if (after != null) {
            query.deploymentAfter(after);
        }
        if (before != null) {
            query.deploymentBefore(before);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(includeDeploymentsWithoutTenantId)) {
            query.includeDeploymentsWithoutTenantId();
        }
        return query;
    }
}

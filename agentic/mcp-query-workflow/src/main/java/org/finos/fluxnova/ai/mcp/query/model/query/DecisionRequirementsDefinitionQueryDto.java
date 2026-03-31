package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.DecisionRequirementsDefinitionQuery;

import java.util.List;

/**
 * DTO for querying decision requirements definitions via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering decision requirements definitions.")
public record DecisionRequirementsDefinitionQueryDto(
        @Schema(description = "Filter by the id of the decision requirements definition.")
        String decisionRequirementsDefinitionId,

        @Schema(description = "Filter by a list of decision requirements definition ids.")
        List<String> decisionRequirementsDefinitionIdIn,

        @Schema(description = "Filter by the category of the decision requirements definition.")
        String category,

        @Schema(description = "Filter by decision requirements definition categories that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %category%.")
        String categoryLike,

        @Schema(description = "Filter by the name of the decision requirements definition.")
        String name,

        @Schema(description = "Filter by decision requirements definition names that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %name%.")
        String nameLike,

        @Schema(description = "Filter by the key of the decision requirements definition, i.e., the id of the DMN 1.0 XML decision requirements definition.")
        String key,

        @Schema(description = "Filter by decision requirements definition keys that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %key%.")
        String keyLike,

        @Schema(description = "Filter by the deployment id the decision requirements definition belongs to.")
        String deploymentId,

        @Schema(description = "Filter by the version of the decision requirements definition.")
        Integer version,

        @Schema(description = "Only include those decision requirements definitions that are latest versions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean latestVersion,

        @Schema(description = "Filter by the name of the decision requirements definition resource. Exact match.")
        String resourceName,

        @Schema(description = "Filter by names of decision requirements definition resources that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %.dmn.")
        String resourceNameLike,

        @Schema(description = "Filter by a list of tenant ids. A decision requirements definition must have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include decision requirements definitions which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Include decision requirements definitions which belong to no tenant. "
                + "Can be used in combination with tenantIdIn.")
        Boolean includeDecisionRequirementsDefinitionsWithoutTenantId
) {
    public DecisionRequirementsDefinitionQuery toQuery(RepositoryService repositoryService) {
        DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();
        if (decisionRequirementsDefinitionId != null) {
            query.decisionRequirementsDefinitionId(decisionRequirementsDefinitionId);
        }
        if (decisionRequirementsDefinitionIdIn != null && !decisionRequirementsDefinitionIdIn.isEmpty()) {
            query.decisionRequirementsDefinitionIdIn(decisionRequirementsDefinitionIdIn.toArray(new String[0]));
        }
        if (category != null) {
            query.decisionRequirementsDefinitionCategory(category);
        }
        if (categoryLike != null) {
            query.decisionRequirementsDefinitionCategoryLike(categoryLike);
        }
        if (name != null) {
            query.decisionRequirementsDefinitionName(name);
        }
        if (nameLike != null) {
            query.decisionRequirementsDefinitionNameLike(nameLike);
        }
        if (key != null) {
            query.decisionRequirementsDefinitionKey(key);
        }
        if (keyLike != null) {
            query.decisionRequirementsDefinitionKeyLike(keyLike);
        }
        if (deploymentId != null) {
            query.deploymentId(deploymentId);
        }
        if (version != null) {
            query.decisionRequirementsDefinitionVersion(version);
        }
        if (Boolean.TRUE.equals(latestVersion)) {
            query.latestVersion();
        }
        if (resourceName != null) {
            query.decisionRequirementsDefinitionResourceName(resourceName);
        }
        if (resourceNameLike != null) {
            query.decisionRequirementsDefinitionResourceNameLike(resourceNameLike);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(includeDecisionRequirementsDefinitionsWithoutTenantId)) {
            query.includeDecisionRequirementsDefinitionsWithoutTenantId();
        }
        return query;
    }
}

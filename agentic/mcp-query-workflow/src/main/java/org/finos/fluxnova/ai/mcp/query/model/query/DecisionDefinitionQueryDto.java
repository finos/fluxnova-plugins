package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.DecisionDefinitionQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying decision definitions via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering decision definitions.")
public record DecisionDefinitionQueryDto(
        @Schema(description = "Filter by the id of the decision definition.")
        String decisionDefinitionId,

        @Schema(description = "Filter by a list of decision definition ids.")
        List<String> decisionDefinitionIdIn,

        @Schema(description = "Filter by the category of the decision definition.")
        String category,

        @Schema(description = "Filter by decision definition categories that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %category%.")
        String categoryLike,

        @Schema(description = "Filter by the name of the decision definition.")
        String name,

        @Schema(description = "Filter by decision definition names that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %name%.")
        String nameLike,

        @Schema(description = "Filter by the key of the decision definition, i.e., the id of the DMN 1.0 XML decision definition.")
        String key,

        @Schema(description = "Filter by decision definition keys that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %key%.")
        String keyLike,

        @Schema(description = "Filter by the deployment id the decision definition belongs to.")
        String deploymentId,

        @Schema(description = "Filter by decision definitions that were deployed after the given date (exclusive). "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date deployedAfter,

        @Schema(description = "Filter by decision definitions that were deployed at the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date deployedAt,

        @Schema(description = "Filter by the version of the decision definition.")
        Integer version,

        @Schema(description = "Only include those decision definitions that are latest versions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean latestVersion,

        @Schema(description = "Filter by the name of the decision definition resource. Exact match.")
        String resourceName,

        @Schema(description = "Filter by names of decision definition resources that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %.dmn.")
        String resourceNameLike,

        @Schema(description = "Filter by the id of the decision requirements definition this decision definition belongs to.")
        String decisionRequirementsDefinitionId,

        @Schema(description = "Filter by the key of the decision requirements definition this decision definition belongs to.")
        String decisionRequirementsDefinitionKey,

        @Schema(description = "Only include decision definitions which belong to no decision requirements definition.")
        Boolean withoutDecisionRequirementsDefinition,

        @Schema(description = "Filter by a list of tenant ids. A decision definition must have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include decision definitions which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Include decision definitions which belong to no tenant. "
                + "Can be used in combination with tenantIdIn.")
        Boolean includeDecisionDefinitionsWithoutTenantId,

        @Schema(description = "Filter by the version tag of the decision definition.")
        String versionTag,

        @Schema(description = "Filter by version tags of decision definitions that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %tag%.")
        String versionTagLike
) {
    public DecisionDefinitionQuery toQuery(RepositoryService repositoryService) {
        DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();
        if (decisionDefinitionId != null) {
            query.decisionDefinitionId(decisionDefinitionId);
        }
        if (decisionDefinitionIdIn != null && !decisionDefinitionIdIn.isEmpty()) {
            query.decisionDefinitionIdIn(decisionDefinitionIdIn.toArray(new String[0]));
        }
        if (category != null) {
            query.decisionDefinitionCategory(category);
        }
        if (categoryLike != null) {
            query.decisionDefinitionCategoryLike(categoryLike);
        }
        if (name != null) {
            query.decisionDefinitionName(name);
        }
        if (nameLike != null) {
            query.decisionDefinitionNameLike(nameLike);
        }
        if (key != null) {
            query.decisionDefinitionKey(key);
        }
        if (keyLike != null) {
            query.decisionDefinitionKeyLike(keyLike);
        }
        if (deploymentId != null) {
            query.deploymentId(deploymentId);
        }
        if (deployedAfter != null) {
            query.deployedAfter(deployedAfter);
        }
        if (deployedAt != null) {
            query.deployedAt(deployedAt);
        }
        if (version != null) {
            query.decisionDefinitionVersion(version);
        }
        if (Boolean.TRUE.equals(latestVersion)) {
            query.latestVersion();
        }
        if (resourceName != null) {
            query.decisionDefinitionResourceName(resourceName);
        }
        if (resourceNameLike != null) {
            query.decisionDefinitionResourceNameLike(resourceNameLike);
        }
        if (decisionRequirementsDefinitionId != null) {
            query.decisionRequirementsDefinitionId(decisionRequirementsDefinitionId);
        }
        if (decisionRequirementsDefinitionKey != null) {
            query.decisionRequirementsDefinitionKey(decisionRequirementsDefinitionKey);
        }
        if (Boolean.TRUE.equals(withoutDecisionRequirementsDefinition)) {
            query.withoutDecisionRequirementsDefinition();
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(includeDecisionDefinitionsWithoutTenantId)) {
            query.includeDecisionDefinitionsWithoutTenantId();
        }
        if (versionTag != null) {
            query.versionTag(versionTag);
        }
        if (versionTagLike != null) {
            query.versionTagLike(versionTagLike);
        }
        return query;
    }
}

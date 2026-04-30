package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.CaseDefinitionQuery;

import java.util.List;

/**
 * DTO for querying case definitions via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering case definitions.")
public record CaseDefinitionQueryDto(
        @Schema(description = "Filter by the id of the case definition.")
        String caseDefinitionId,

        @Schema(description = "Filter by a list of case definition ids.")
        List<String> caseDefinitionIdIn,

        @Schema(description = "Filter by the category of the case definition.")
        String category,

        @Schema(description = "Filter by case definition categories that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %activiti%.")
        String categoryLike,

        @Schema(description = "Filter by the name of the case definition.")
        String name,

        @Schema(description = "Filter by case definition names that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %name%.")
        String nameLike,

        @Schema(description = "Filter by the key of the case definition, i.e., the id of the CMMN 2.0 XML case definition.")
        String key,

        @Schema(description = "Filter by case definition keys that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %key%.")
        String keyLike,

        @Schema(description = "Filter by the deployment id the case definition belongs to.")
        String deploymentId,

        @Schema(description = "Filter by the version of the case definition.")
        Integer version,

        @Schema(description = "Only include those case definitions that are latest versions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean latestVersion,

        @Schema(description = "Filter by the name of the case definition resource. Exact match.")
        String resourceName,

        @Schema(description = "Filter by names of case definition resources that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %.cmmn.")
        String resourceNameLike,

        @Schema(description = "Filter by a list of tenant ids. A case definition must have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include case definitions which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Include case definitions which belong to no tenant. "
                + "Can be used in combination with tenantIdIn.")
        Boolean includeCaseDefinitionsWithoutTenantId
) {
    public CaseDefinitionQuery toQuery(RepositoryService repositoryService) {
        CaseDefinitionQuery query = repositoryService.createCaseDefinitionQuery();
        if (caseDefinitionId != null) {
            query.caseDefinitionId(caseDefinitionId);
        }
        if (caseDefinitionIdIn != null && !caseDefinitionIdIn.isEmpty()) {
            query.caseDefinitionIdIn(caseDefinitionIdIn.toArray(new String[0]));
        }
        if (category != null) {
            query.caseDefinitionCategory(category);
        }
        if (categoryLike != null) {
            query.caseDefinitionCategoryLike(categoryLike);
        }
        if (name != null) {
            query.caseDefinitionName(name);
        }
        if (nameLike != null) {
            query.caseDefinitionNameLike(nameLike);
        }
        if (key != null) {
            query.caseDefinitionKey(key);
        }
        if (keyLike != null) {
            query.caseDefinitionKeyLike(keyLike);
        }
        if (deploymentId != null) {
            query.deploymentId(deploymentId);
        }
        if (version != null) {
            query.caseDefinitionVersion(version);
        }
        if (Boolean.TRUE.equals(latestVersion)) {
            query.latestVersion();
        }
        if (resourceName != null) {
            query.caseDefinitionResourceName(resourceName);
        }
        if (resourceNameLike != null) {
            query.caseDefinitionResourceNameLike(resourceNameLike);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(includeCaseDefinitionsWithoutTenantId)) {
            query.includeCaseDefinitionsWithoutTenantId();
        }
        return query;
    }
}

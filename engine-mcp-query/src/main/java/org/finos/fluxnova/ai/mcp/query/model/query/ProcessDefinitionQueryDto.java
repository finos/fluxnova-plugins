package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinitionQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying process definitions via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering process definitions.")
public record ProcessDefinitionQueryDto(
        @Schema(description = "Filter by process definition id.")
        String processDefinitionId,

        @Schema(description = "Filter by a list of process definition ids.")
        List<String> processDefinitionIdIn,

        @Schema(description = "Filter by the category of the process definition.")
        String category,

        @Schema(description = "Filter by process definition categories that the parameter is a substring of.")
        String categoryLike,

        @Schema(description = "Filter by the name of the process definition.")
        String name,

        @Schema(description = "Filter by process definition names that the parameter is a substring of.")
        String nameLike,

        @Schema(description = "Filter by the deployment id of the process definition.")
        String deploymentId,

        @Schema(description = "Filter by a deployment date after the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date deployedAfter,

        @Schema(description = "Filter by a deployment date at the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date deployedAt,

        @Schema(description = "Filter by the key of the process definition.")
        String key,

        @Schema(description = "Filter by process definition keys that the parameter is a substring of.")
        String keyLike,

        @Schema(description = "Filter by the version of the process definition.")
        Integer version,

        @Schema(description = "Only include those process definitions that are latest versions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean latestVersion,

        @Schema(description = "Filter by the name of the process definition resource. Exact match.")
        String resourceName,

        @Schema(description = "Filter by names of process definition resources that the parameter is a substring of.")
        String resourceNameLike,

        @Schema(description = "Filter by a user name who is allowed to start the process.")
        String startableBy,

        @Schema(description = "Only include active process definitions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include suspended process definitions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean suspended,

        @Schema(description = "Filter by the incident id.")
        String incidentId,

        @Schema(description = "Filter by the incident type.", allowableValues = {"failedJob", "failedExternalTask"})
        String incidentType,

        @Schema(description = "Filter by the incident message. Exact match.")
        String incidentMessage,

        @Schema(description = "Filter by the incident message that the parameter is a substring of.")
        String incidentMessageLike,

        @Schema(description = "Filter by a list of tenant ids. A process definition must have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include process definitions which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Include process definitions which belong to no tenant. "
                + "Can be used in combination with tenantIdIn.")
        Boolean includeProcessDefinitionsWithoutTenantId,

        @Schema(description = "Filter by the version tag of the process definition.")
        String versionTag,

        @Schema(description = "Filter by version tags of process definitions that the parameter is a substring of.")
        String versionTagLike,

        @Schema(description = "Only include process definitions without a version tag.")
        Boolean withoutVersionTag,

        @Schema(description = "Filter by process definitions which are startable in Tasklist.")
        Boolean startableInTasklist,

        @Schema(description = "Filter by process definitions which are not startable in Tasklist.")
        Boolean notStartableInTasklist
) {
    /**
     * Create a new ProcessDefinitionQuery from the RepositoryService, with all non-null filter criteria applied.
     */
    public ProcessDefinitionQuery toQuery(RepositoryService repositoryService) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (processDefinitionIdIn != null && !processDefinitionIdIn.isEmpty()) {
            query.processDefinitionIdIn(processDefinitionIdIn.toArray(new String[0]));
        }
        if (category != null) {
            query.processDefinitionCategory(category);
        }
        if (categoryLike != null) {
            query.processDefinitionCategoryLike(categoryLike);
        }
        if (name != null) {
            query.processDefinitionName(name);
        }
        if (nameLike != null) {
            query.processDefinitionNameLike(nameLike);
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
        if (key != null) {
            query.processDefinitionKey(key);
        }
        if (keyLike != null) {
            query.processDefinitionKeyLike(keyLike);
        }
        if (version != null) {
            query.processDefinitionVersion(version);
        }
        if (Boolean.TRUE.equals(latestVersion)) {
            query.latestVersion();
        }
        if (resourceName != null) {
            query.processDefinitionResourceName(resourceName);
        }
        if (resourceNameLike != null) {
            query.processDefinitionResourceNameLike(resourceNameLike);
        }
        if (startableBy != null) {
            query.startableByUser(startableBy);
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        if (incidentId != null) {
            query.incidentId(incidentId);
        }
        if (incidentType != null) {
            query.incidentType(incidentType);
        }
        if (incidentMessage != null) {
            query.incidentMessage(incidentMessage);
        }
        if (incidentMessageLike != null) {
            query.incidentMessageLike(incidentMessageLike);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(includeProcessDefinitionsWithoutTenantId)) {
            query.includeProcessDefinitionsWithoutTenantId();
        }
        if (versionTag != null) {
            query.versionTag(versionTag);
        }
        if (versionTagLike != null) {
            query.versionTagLike(versionTagLike);
        }
        if (Boolean.TRUE.equals(withoutVersionTag)) {
            query.withoutVersionTag();
        }
        if (Boolean.TRUE.equals(startableInTasklist)) {
            query.startableInTasklist();
        }
        if (Boolean.TRUE.equals(notStartableInTasklist)) {
            query.notStartableInTasklist();
        }
        return query;
    }
}

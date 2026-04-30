package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.management.JobDefinitionQuery;

import java.util.List;

/**
 * DTO for querying job definitions via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering job definitions.")
public record JobDefinitionQueryDto(
        @Schema(description = "Filter by the id of the job definition.")
        String jobDefinitionId,

        @Schema(description = "Filter by a list of activity ids. A job definition must be associated with one of the given activities.")
        List<String> activityIdIn,

        @Schema(description = "Filter by the id of the process definition the job definition belongs to.")
        String processDefinitionId,

        @Schema(description = "Filter by the key of the process definition the job definition belongs to.")
        String processDefinitionKey,

        @Schema(description = "Filter by the job type of the job definition "
                + "(e.g. timer-start-event, message-intermediate-catching-event, async-continuation).")
        String jobType,

        @Schema(description = "Filter by the configuration of the job definition. "
                + "For timer jobs this contains the timer configuration.")
        String jobConfiguration,

        @Schema(description = "Only include active (not suspended) job definitions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include suspended job definitions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean suspended,

        @Schema(description = "Only include job definitions that have an overriding job priority defined. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withOverridingJobPriority,

        @Schema(description = "Filter by a list of tenant ids. A job definition must belong to one of the given tenants.")
        List<String> tenantIdIn,

        @Schema(description = "Only include job definitions which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Include job definitions that belong to no tenant in the results. "
                + "Can be used in combination with tenantIdIn.")
        Boolean includeJobDefinitionsWithoutTenantId
) {
    public JobDefinitionQuery toQuery(ManagementService managementService) {
        JobDefinitionQuery query = managementService.createJobDefinitionQuery();
        if (jobDefinitionId != null) {
            query.jobDefinitionId(jobDefinitionId);
        }
        if (activityIdIn != null && !activityIdIn.isEmpty()) {
            query.activityIdIn(activityIdIn.toArray(new String[0]));
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (processDefinitionKey != null) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (jobType != null) {
            query.jobType(jobType);
        }
        if (jobConfiguration != null) {
            query.jobConfiguration(jobConfiguration);
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        if (Boolean.TRUE.equals(withOverridingJobPriority)) {
            query.withOverridingJobPriority();
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(includeJobDefinitionsWithoutTenantId)) {
            query.includeJobDefinitionsWithoutTenantId();
        }
        return query;
    }
}

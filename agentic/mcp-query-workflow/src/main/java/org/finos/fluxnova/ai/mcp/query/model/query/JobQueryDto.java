package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.runtime.JobQuery;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * DTO for querying jobs via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering jobs.")
public record JobQueryDto(
        @Schema(description = "Filter by the id of the job.")
        String jobId,

        @Schema(description = "Filter by a list of job ids.")
        List<String> jobIds,

        @Schema(description = "Filter by the id of the job definition the job belongs to.")
        String jobDefinitionId,

        @Schema(description = "Filter by the id of the root process instance the job belongs to.")
        String rootProcessInstanceId,

        @Schema(description = "Filter by the id of the process instance the job belongs to.")
        String processInstanceId,

        @Schema(description = "Filter by a list of process instance ids. A job must belong to one of the given process instances.")
        List<String> processInstanceIds,

        @Schema(description = "Filter by the id of the process definition the job belongs to.")
        String processDefinitionId,

        @Schema(description = "Filter by the key of the process definition the job belongs to.")
        String processDefinitionKey,

        @Schema(description = "Filter by the id of the execution the job belongs to.")
        String executionId,

        @Schema(description = "Filter by the id of the activity the job is defined on.")
        String activityId,

        @Schema(description = "Only include jobs that have retries left (retries > 0). "
                + "Value may only be true, as false is the default behavior.")
        Boolean withRetriesLeft,

        @Schema(description = "Only include jobs that have no retries left (retries = 0). "
                + "Value may only be true, as false is the default behavior.")
        Boolean noRetriesLeft,

        @Schema(description = "Only include jobs that are executable (retries > 0 and due date is null or in the past). "
                + "Value may only be true, as false is the default behavior.")
        Boolean executable,

        @Schema(description = "Only include timer jobs. Cannot be used together with messages. "
                + "Value may only be true, as false is the default behavior.")
        Boolean timers,

        @Schema(description = "Only include message jobs. Cannot be used together with timers. "
                + "Value may only be true, as false is the default behavior.")
        Boolean messages,

        @Schema(description = "Only include jobs whose due date is before the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date duedateLowerThan,

        @Schema(description = "Only include jobs whose due date is after the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date duedateHigherThan,

        @Schema(description = "Only include jobs created before the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date createdBefore,

        @Schema(description = "Only include jobs created after the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date createdAfter,

        @Schema(description = "Only include jobs with a priority that is higher than or equal to the given value.")
        Long priorityHigherThanOrEquals,

        @Schema(description = "Only include jobs with a priority that is lower than or equal to the given value.")
        Long priorityLowerThanOrEquals,

        @Schema(description = "Only include jobs that failed due to an exception. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withException,

        @Schema(description = "Only include jobs that failed due to an exception with the given message.")
        String exceptionMessage,

        @Schema(description = "Only include jobs that failed at the activity with the given id.")
        String failedActivityId,

        @Schema(description = "Only include active (not suspended) jobs. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include suspended jobs. "
                + "Value may only be true, as false is the default behavior.")
        Boolean suspended,

        @Schema(description = "Filter by a list of tenant ids. A job must belong to one of the given tenants.")
        List<String> tenantIdIn,

        @Schema(description = "Only include jobs which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Include jobs that belong to no tenant in the results. "
                + "Can be used in combination with tenantIdIn.")
        Boolean includeJobsWithoutTenantId
) {
    public JobQuery toQuery(ManagementService managementService) {
        JobQuery query = managementService.createJobQuery();
        if (jobId != null) {
            query.jobId(jobId);
        }
        if (jobIds != null && !jobIds.isEmpty()) {
            query.jobIds(new HashSet<>(jobIds));
        }
        if (jobDefinitionId != null) {
            query.jobDefinitionId(jobDefinitionId);
        }
        if (rootProcessInstanceId != null) {
            query.rootProcessInstanceId(rootProcessInstanceId);
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (processInstanceIds != null && !processInstanceIds.isEmpty()) {
            query.processInstanceIds(new HashSet<>(processInstanceIds));
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (processDefinitionKey != null) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (activityId != null) {
            query.activityId(activityId);
        }
        if (Boolean.TRUE.equals(withRetriesLeft)) {
            query.withRetriesLeft();
        }
        if (Boolean.TRUE.equals(noRetriesLeft)) {
            query.noRetriesLeft();
        }
        if (Boolean.TRUE.equals(executable)) {
            query.executable();
        }
        if (Boolean.TRUE.equals(timers)) {
            query.timers();
        }
        if (Boolean.TRUE.equals(messages)) {
            query.messages();
        }
        if (duedateLowerThan != null) {
            query.duedateLowerThan(duedateLowerThan);
        }
        if (duedateHigherThan != null) {
            query.duedateHigherThan(duedateHigherThan);
        }
        if (createdBefore != null) {
            query.createdBefore(createdBefore);
        }
        if (createdAfter != null) {
            query.createdAfter(createdAfter);
        }
        if (priorityHigherThanOrEquals != null) {
            query.priorityHigherThanOrEquals(priorityHigherThanOrEquals);
        }
        if (priorityLowerThanOrEquals != null) {
            query.priorityLowerThanOrEquals(priorityLowerThanOrEquals);
        }
        if (Boolean.TRUE.equals(withException)) {
            query.withException();
        }
        if (exceptionMessage != null) {
            query.exceptionMessage(exceptionMessage);
        }
        if (failedActivityId != null) {
            query.failedActivityId(failedActivityId);
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(includeJobsWithoutTenantId)) {
            query.includeJobsWithoutTenantId();
        }
        return query;
    }
}

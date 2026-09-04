package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricJobLogQuery;

import java.util.List;

/**
 * DTO for querying historic job logs.
 */
@Schema(description = "Parameters for querying historic job logs.")
public record HistoricJobLogQueryDto(
        @Schema(description = "Filter by the id of the historic job log entry.") String logId,
        @Schema(description = "Filter by the id of the job.") String jobId,
        @Schema(description = "Filter by the exception message of the job.") String jobExceptionMessage,
        @Schema(description = "Filter by the id of the job definition.") String jobDefinitionId,
        @Schema(description = "Filter by the type of the job definition.") String jobDefinitionType,
        @Schema(description = "Filter by the configuration of the job definition.") String jobDefinitionConfiguration,
        @Schema(description = "Filter by a list of activity ids.") List<String> activityIdIn,
        @Schema(description = "Filter by a list of failed activity ids.") List<String> failedActivityIdIn,
        @Schema(description = "Filter by a list of execution ids.") List<String> executionIdIn,
        @Schema(description = "Filter by the id of the process instance.") String processInstanceId,
        @Schema(description = "Filter by the id of the process definition.") String processDefinitionId,
        @Schema(description = "Filter by the key of the process definition.") String processDefinitionKey,
        @Schema(description = "Filter by the id of the deployment.") String deploymentId,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only log entries without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Filter by the hostname of the worker that executed the job.") String hostname,
        @Schema(description = "Only return log entries with a job priority higher than or equal to this value.") Long jobPriorityHigherThanOrEquals,
        @Schema(description = "Only return log entries with a job priority lower than or equal to this value.") Long jobPriorityLowerThanOrEquals,
        @Schema(description = "If true, only creation log entries are returned.") Boolean creationLog,
        @Schema(description = "If true, only failure log entries are returned.") Boolean failureLog,
        @Schema(description = "If true, only success log entries are returned.") Boolean successLog,
        @Schema(description = "If true, only deletion log entries are returned.") Boolean deletionLog,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricJobLogQuery toQuery(HistoricJobLogQuery query) {
        if (logId != null) query.logId(logId);
        if (jobId != null) query.jobId(jobId);
        if (jobExceptionMessage != null) query.jobExceptionMessage(jobExceptionMessage);
        if (jobDefinitionId != null) query.jobDefinitionId(jobDefinitionId);
        if (jobDefinitionType != null) query.jobDefinitionType(jobDefinitionType);
        if (jobDefinitionConfiguration != null) query.jobDefinitionConfiguration(jobDefinitionConfiguration);
        if (activityIdIn != null && !activityIdIn.isEmpty())
            query.activityIdIn(activityIdIn.toArray(new String[0]));
        if (failedActivityIdIn != null && !failedActivityIdIn.isEmpty())
            query.failedActivityIdIn(failedActivityIdIn.toArray(new String[0]));
        if (executionIdIn != null && !executionIdIn.isEmpty())
            query.executionIdIn(executionIdIn.toArray(new String[0]));
        if (processInstanceId != null) query.processInstanceId(processInstanceId);
        if (processDefinitionId != null) query.processDefinitionId(processDefinitionId);
        if (processDefinitionKey != null) query.processDefinitionKey(processDefinitionKey);
        if (deploymentId != null) query.deploymentId(deploymentId);
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        if (hostname != null) query.hostname(hostname);
        if (jobPriorityHigherThanOrEquals != null) query.jobPriorityHigherThanOrEquals(jobPriorityHigherThanOrEquals);
        if (jobPriorityLowerThanOrEquals != null) query.jobPriorityLowerThanOrEquals(jobPriorityLowerThanOrEquals);
        if (Boolean.TRUE.equals(creationLog)) query.creationLog();
        if (Boolean.TRUE.equals(failureLog)) query.failureLog();
        if (Boolean.TRUE.equals(successLog)) query.successLog();
        if (Boolean.TRUE.equals(deletionLog)) query.deletionLog();
        return query;
    }
}

package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricJobLog;

import java.util.Date;

/**
 * DTO representing a single historic job log entry result.
 */
@Schema(description = "A historic job log entry.")
public record HistoricJobLogResultDto(
        @Schema(description = "The id of the historic job log entry.") String id,
        @Schema(description = "The time the log entry was created.") Date timestamp,
        @Schema(description = "The id of the job.") String jobId,
        @Schema(description = "The due date of the job at the time this log was created.") Date jobDueDate,
        @Schema(description = "The number of retries the job had at the time this log was created.") int jobRetries,
        @Schema(description = "The priority of the job at the time this log was created.") long jobPriority,
        @Schema(description = "The exception message if the job failed.") String jobExceptionMessage,
        @Schema(description = "The id of the job definition.") String jobDefinitionId,
        @Schema(description = "The type of the job definition.") String jobDefinitionType,
        @Schema(description = "The configuration of the job definition.") String jobDefinitionConfiguration,
        @Schema(description = "The id of the activity the job is associated with.") String activityId,
        @Schema(description = "The id of the activity where the job failed.") String failedActivityId,
        @Schema(description = "The id of the execution.") String executionId,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The id of the process instance.") String processInstanceId,
        @Schema(description = "The id of the process definition.") String processDefinitionId,
        @Schema(description = "The key of the process definition.") String processDefinitionKey,
        @Schema(description = "The id of the deployment.") String deploymentId,
        @Schema(description = "The id of the tenant.") String tenantId,
        @Schema(description = "The hostname of the worker that executed the job.") String hostname,
        @Schema(description = "Whether this log entry records a job creation.") boolean creationLog,
        @Schema(description = "Whether this log entry records a job failure.") boolean failureLog,
        @Schema(description = "Whether this log entry records a job success.") boolean successLog,
        @Schema(description = "Whether this log entry records a job deletion.") boolean deletionLog,
        @Schema(description = "The time this historic job log entry will be removed.") Date removalTime
) {
    public static HistoricJobLogResultDto fromHistoricJobLog(HistoricJobLog historicJobLog) {
        return new HistoricJobLogResultDto(
                historicJobLog.getId(),
                historicJobLog.getTimestamp(),
                historicJobLog.getJobId(),
                historicJobLog.getJobDueDate(),
                historicJobLog.getJobRetries(),
                historicJobLog.getJobPriority(),
                historicJobLog.getJobExceptionMessage(),
                historicJobLog.getJobDefinitionId(),
                historicJobLog.getJobDefinitionType(),
                historicJobLog.getJobDefinitionConfiguration(),
                historicJobLog.getActivityId(),
                historicJobLog.getFailedActivityId(),
                historicJobLog.getExecutionId(),
                historicJobLog.getRootProcessInstanceId(),
                historicJobLog.getProcessInstanceId(),
                historicJobLog.getProcessDefinitionId(),
                historicJobLog.getProcessDefinitionKey(),
                historicJobLog.getDeploymentId(),
                historicJobLog.getTenantId(),
                historicJobLog.getHostname(),
                historicJobLog.isCreationLog(),
                historicJobLog.isFailureLog(),
                historicJobLog.isSuccessLog(),
                historicJobLog.isDeletionLog(),
                historicJobLog.getRemovalTime()
        );
    }
}

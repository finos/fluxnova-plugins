package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.runtime.Job;

import java.util.Date;

/**
 * Result DTO representing a job.
 */
@Schema(description = "Represents a scheduled or asynchronous job in the process engine.")
public record JobResultDto(
        @Schema(description = "The id of the job.")
        String id,

        @Schema(description = "The due date of the job.")
        Date duedate,

        @Schema(description = "The id of the root process instance the job belongs to.")
        String rootProcessInstanceId,

        @Schema(description = "The id of the process instance the job belongs to.")
        String processInstanceId,

        @Schema(description = "The id of the process definition the job belongs to.")
        String processDefinitionId,

        @Schema(description = "The key of the process definition the job belongs to.")
        String processDefinitionKey,

        @Schema(description = "The id of the execution the job belongs to.")
        String executionId,

        @Schema(description = "The id of the job definition this job was created from.")
        String jobDefinitionId,

        @Schema(description = "The id of the deployment the job belongs to.")
        String deploymentId,

        @Schema(description = "The number of retries this job has left.")
        int retries,

        @Schema(description = "The message of the exception that occurred the last time the job was executed.")
        String exceptionMessage,

        @Schema(description = "The id of the activity on which the last exception occurred.")
        String failedActivityId,

        @Schema(description = "Whether this job is currently suspended.")
        boolean suspended,

        @Schema(description = "The job's priority.")
        long priority,

        @Schema(description = "The id of the tenant this job belongs to.")
        String tenantId,

        @Schema(description = "The date/time when this job was created.")
        Date createTime
) {
    public static JobResultDto fromJob(Job job) {
        return new JobResultDto(
                job.getId(),
                job.getDuedate(),
                job.getRootProcessInstanceId(),
                job.getProcessInstanceId(),
                job.getProcessDefinitionId(),
                job.getProcessDefinitionKey(),
                job.getExecutionId(),
                job.getJobDefinitionId(),
                job.getDeploymentId(),
                job.getRetries(),
                job.getExceptionMessage(),
                job.getFailedActivityId(),
                job.isSuspended(),
                job.getPriority(),
                job.getTenantId(),
                job.getCreateTime()
        );
    }
}

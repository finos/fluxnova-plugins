package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.UserOperationLogEntry;

import java.util.Date;

/**
 * DTO representing a single user operation log entry result.
 */
@Schema(description = "A user operation log entry recording a change made by a user.")
public record UserOperationLogEntryResultDto(
        @Schema(description = "The id of the log entry.") String id,
        @Schema(description = "The id of the deployment.") String deploymentId,
        @Schema(description = "The id of the process definition.") String processDefinitionId,
        @Schema(description = "The key of the process definition.") String processDefinitionKey,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The id of the process instance.") String processInstanceId,
        @Schema(description = "The id of the execution.") String executionId,
        @Schema(description = "The id of the case definition.") String caseDefinitionId,
        @Schema(description = "The id of the case instance.") String caseInstanceId,
        @Schema(description = "The id of the case execution.") String caseExecutionId,
        @Schema(description = "The id of the task.") String taskId,
        @Schema(description = "The id of the job.") String jobId,
        @Schema(description = "The id of the job definition.") String jobDefinitionId,
        @Schema(description = "The id of the batch.") String batchId,
        @Schema(description = "The id of the user who performed the operation.") String userId,
        @Schema(description = "The timestamp when the operation was performed.") Date timestamp,
        @Schema(description = "The id of the operation.") String operationId,
        @Schema(description = "The id of the external task.") String externalTaskId,
        @Schema(description = "The type of the operation.") String operationType,
        @Schema(description = "The type of entity affected.") String entityType,
        @Schema(description = "The property that was changed.") String property,
        @Schema(description = "The original value of the property before the change.") String orgValue,
        @Schema(description = "The new value of the property after the change.") String newValue,
        @Schema(description = "The time this log entry will be removed.") Date removalTime,
        @Schema(description = "The category of the operation.") String category,
        @Schema(description = "An annotation added to this log entry.") String annotation,
        @Schema(description = "The id of the tenant.") String tenantId
) {
    public static UserOperationLogEntryResultDto fromUserOperationLogEntry(UserOperationLogEntry userOperationLogEntry) {
        return new UserOperationLogEntryResultDto(
                userOperationLogEntry.getId(),
                userOperationLogEntry.getDeploymentId(),
                userOperationLogEntry.getProcessDefinitionId(),
                userOperationLogEntry.getProcessDefinitionKey(),
                userOperationLogEntry.getRootProcessInstanceId(),
                userOperationLogEntry.getProcessInstanceId(),
                userOperationLogEntry.getExecutionId(),
                userOperationLogEntry.getCaseDefinitionId(),
                userOperationLogEntry.getCaseInstanceId(),
                userOperationLogEntry.getCaseExecutionId(),
                userOperationLogEntry.getTaskId(),
                userOperationLogEntry.getJobId(),
                userOperationLogEntry.getJobDefinitionId(),
                userOperationLogEntry.getBatchId(),
                userOperationLogEntry.getUserId(),
                userOperationLogEntry.getTimestamp(),
                userOperationLogEntry.getOperationId(),
                userOperationLogEntry.getExternalTaskId(),
                userOperationLogEntry.getOperationType(),
                userOperationLogEntry.getEntityType(),
                userOperationLogEntry.getProperty(),
                userOperationLogEntry.getOrgValue(),
                userOperationLogEntry.getNewValue(),
                userOperationLogEntry.getRemovalTime(),
                userOperationLogEntry.getCategory(),
                userOperationLogEntry.getAnnotation(),
                userOperationLogEntry.getTenantId()
        );
    }
}

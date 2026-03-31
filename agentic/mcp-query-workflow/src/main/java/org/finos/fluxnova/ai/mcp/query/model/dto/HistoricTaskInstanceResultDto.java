package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricTaskInstance;

import java.util.Date;

/**
 * DTO representing a single historic task instance result.
 */
@Schema(description = "A historic task instance.")
public record HistoricTaskInstanceResultDto(
        @Schema(description = "The id of the historic task instance.") String id,
        @Schema(description = "The key of the process definition.") String processDefinitionKey,
        @Schema(description = "The id of the process definition.") String processDefinitionId,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The id of the process instance.") String processInstanceId,
        @Schema(description = "The id of the execution.") String executionId,
        @Schema(description = "The key of the case definition.") String caseDefinitionKey,
        @Schema(description = "The id of the case definition.") String caseDefinitionId,
        @Schema(description = "The id of the case instance.") String caseInstanceId,
        @Schema(description = "The id of the case execution.") String caseExecutionId,
        @Schema(description = "The id of the activity instance.") String activityInstanceId,
        @Schema(description = "The name of the task.") String name,
        @Schema(description = "The description of the task.") String description,
        @Schema(description = "The reason for deletion, if deleted.") String deleteReason,
        @Schema(description = "The owner of the task.") String owner,
        @Schema(description = "The assignee of the task.") String assignee,
        @Schema(description = "The time the task was started (created).") Date startTime,
        @Schema(description = "The time the task ended.") Date endTime,
        @Schema(description = "The duration of the task in milliseconds.") Long durationInMillis,
        @Schema(description = "The task definition key.") String taskDefinitionKey,
        @Schema(description = "The priority of the task.") int priority,
        @Schema(description = "The due date of the task.") Date dueDate,
        @Schema(description = "The id of the parent task.") String parentTaskId,
        @Schema(description = "The follow-up date of the task.") Date followUpDate,
        @Schema(description = "The id of the tenant this task belongs to.") String tenantId,
        @Schema(description = "The time this historic task instance will be removed.") Date removalTime,
        @Schema(description = "The state of the task.") String taskState
) {
    public static HistoricTaskInstanceResultDto fromHistoricTaskInstance(HistoricTaskInstance historicTaskInstance) {
        return new HistoricTaskInstanceResultDto(
                historicTaskInstance.getId(),
                historicTaskInstance.getProcessDefinitionKey(),
                historicTaskInstance.getProcessDefinitionId(),
                historicTaskInstance.getRootProcessInstanceId(),
                historicTaskInstance.getProcessInstanceId(),
                historicTaskInstance.getExecutionId(),
                historicTaskInstance.getCaseDefinitionKey(),
                historicTaskInstance.getCaseDefinitionId(),
                historicTaskInstance.getCaseInstanceId(),
                historicTaskInstance.getCaseExecutionId(),
                historicTaskInstance.getActivityInstanceId(),
                historicTaskInstance.getName(),
                historicTaskInstance.getDescription(),
                historicTaskInstance.getDeleteReason(),
                historicTaskInstance.getOwner(),
                historicTaskInstance.getAssignee(),
                historicTaskInstance.getStartTime(),
                historicTaskInstance.getEndTime(),
                historicTaskInstance.getDurationInMillis(),
                historicTaskInstance.getTaskDefinitionKey(),
                historicTaskInstance.getPriority(),
                historicTaskInstance.getDueDate(),
                historicTaskInstance.getParentTaskId(),
                historicTaskInstance.getFollowUpDate(),
                historicTaskInstance.getTenantId(),
                historicTaskInstance.getRemovalTime(),
                historicTaskInstance.getTaskState()
        );
    }
}

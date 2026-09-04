package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricActivityInstance;

import java.util.Date;

/**
 * DTO representing a single historic activity instance result.
 */
@Schema(description = "A historic activity instance, representing one execution of a BPMN activity.")
public record HistoricActivityInstanceResultDto(
        @Schema(description = "The id of the historic activity instance.") String id,
        @Schema(description = "The id of the parent activity instance.") String parentActivityInstanceId,
        @Schema(description = "The id of the activity in the process definition.") String activityId,
        @Schema(description = "The name of the activity.") String activityName,
        @Schema(description = "The type of the activity (e.g. userTask, serviceTask).") String activityType,
        @Schema(description = "The key of the process definition.") String processDefinitionKey,
        @Schema(description = "The id of the process definition.") String processDefinitionId,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The id of the process instance.") String processInstanceId,
        @Schema(description = "The id of the execution.") String executionId,
        @Schema(description = "The id of the task, if this is a user task activity.") String taskId,
        @Schema(description = "The id of the called process instance, if this is a call activity.") String calledProcessInstanceId,
        @Schema(description = "The id of the called case instance, if this is a case call activity.") String calledCaseInstanceId,
        @Schema(description = "The assignee of the task, if applicable.") String assignee,
        @Schema(description = "The time the activity instance was started.") Date startTime,
        @Schema(description = "The time the activity instance ended.") Date endTime,
        @Schema(description = "The duration of the activity in milliseconds.") Long durationInMillis,
        @Schema(description = "Whether the activity instance completed its scope.") boolean completeScope,
        @Schema(description = "Whether the activity instance was canceled.") boolean canceled,
        @Schema(description = "The id of the tenant this activity instance belongs to.") String tenantId,
        @Schema(description = "The time this historic activity instance will be removed.") Date removalTime
) {
    public static HistoricActivityInstanceResultDto fromHistoricActivityInstance(HistoricActivityInstance historicActivityInstance) {
        return new HistoricActivityInstanceResultDto(
                historicActivityInstance.getId(),
                historicActivityInstance.getParentActivityInstanceId(),
                historicActivityInstance.getActivityId(),
                historicActivityInstance.getActivityName(),
                historicActivityInstance.getActivityType(),
                historicActivityInstance.getProcessDefinitionKey(),
                historicActivityInstance.getProcessDefinitionId(),
                historicActivityInstance.getRootProcessInstanceId(),
                historicActivityInstance.getProcessInstanceId(),
                historicActivityInstance.getExecutionId(),
                historicActivityInstance.getTaskId(),
                historicActivityInstance.getCalledProcessInstanceId(),
                historicActivityInstance.getCalledCaseInstanceId(),
                historicActivityInstance.getAssignee(),
                historicActivityInstance.getStartTime(),
                historicActivityInstance.getEndTime(),
                historicActivityInstance.getDurationInMillis(),
                historicActivityInstance.isCompleteScope(),
                historicActivityInstance.isCanceled(),
                historicActivityInstance.getTenantId(),
                historicActivityInstance.getRemovalTime()
        );
    }
}

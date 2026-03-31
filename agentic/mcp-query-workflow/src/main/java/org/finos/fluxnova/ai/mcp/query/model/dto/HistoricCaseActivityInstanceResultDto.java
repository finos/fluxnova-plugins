package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricCaseActivityInstance;

import java.util.Date;

/**
 * DTO representing a single historic case activity instance result.
 */
@Schema(description = "A historic case activity instance.")
public record HistoricCaseActivityInstanceResultDto(
        @Schema(description = "The id of the historic case activity instance.") String id,
        @Schema(description = "The id of the parent case activity instance.") String parentCaseActivityInstanceId,
        @Schema(description = "The id of the case activity.") String caseActivityId,
        @Schema(description = "The name of the case activity.") String caseActivityName,
        @Schema(description = "The type of the case activity.") String caseActivityType,
        @Schema(description = "The id of the case definition.") String caseDefinitionId,
        @Schema(description = "The id of the case instance.") String caseInstanceId,
        @Schema(description = "The id of the case execution.") String caseExecutionId,
        @Schema(description = "The id of the task created for this case activity instance.") String taskId,
        @Schema(description = "The id of the process instance called by this case activity instance.") String calledProcessInstanceId,
        @Schema(description = "The id of the case instance called by this case activity instance.") String calledCaseInstanceId,
        @Schema(description = "The id of the tenant.") String tenantId,
        @Schema(description = "The time this instance was created.") Date createTime,
        @Schema(description = "The time this instance ended.") Date endTime,
        @Schema(description = "The duration of this instance in milliseconds.") Long durationInMillis,
        @Schema(description = "Whether this case activity instance is required.") boolean required,
        @Schema(description = "Whether this case activity instance is available.") boolean available,
        @Schema(description = "Whether this case activity instance is enabled.") boolean enabled,
        @Schema(description = "Whether this case activity instance is disabled.") boolean disabled,
        @Schema(description = "Whether this case activity instance is active.") boolean active,
        @Schema(description = "Whether this case activity instance is completed.") boolean completed,
        @Schema(description = "Whether this case activity instance is terminated.") boolean terminated
) {
    public static HistoricCaseActivityInstanceResultDto fromHistoricCaseActivityInstance(HistoricCaseActivityInstance historicCaseActivityInstance) {
        return new HistoricCaseActivityInstanceResultDto(
                historicCaseActivityInstance.getId(),
                historicCaseActivityInstance.getParentCaseActivityInstanceId(),
                historicCaseActivityInstance.getCaseActivityId(),
                historicCaseActivityInstance.getCaseActivityName(),
                historicCaseActivityInstance.getCaseActivityType(),
                historicCaseActivityInstance.getCaseDefinitionId(),
                historicCaseActivityInstance.getCaseInstanceId(),
                historicCaseActivityInstance.getCaseExecutionId(),
                historicCaseActivityInstance.getTaskId(),
                historicCaseActivityInstance.getCalledProcessInstanceId(),
                historicCaseActivityInstance.getCalledCaseInstanceId(),
                historicCaseActivityInstance.getTenantId(),
                historicCaseActivityInstance.getCreateTime(),
                historicCaseActivityInstance.getEndTime(),
                historicCaseActivityInstance.getDurationInMillis(),
                historicCaseActivityInstance.isRequired(),
                historicCaseActivityInstance.isAvailable(),
                historicCaseActivityInstance.isEnabled(),
                historicCaseActivityInstance.isDisabled(),
                historicCaseActivityInstance.isActive(),
                historicCaseActivityInstance.isCompleted(),
                historicCaseActivityInstance.isTerminated()
        );
    }
}

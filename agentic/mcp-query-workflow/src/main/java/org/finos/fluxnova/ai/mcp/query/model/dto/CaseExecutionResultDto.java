package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.runtime.CaseExecution;

/**
 * Result DTO representing a case execution.
 */
@Schema(description = "Represents a planned item (stage, milestone, task) within a case instance.")
public record CaseExecutionResultDto(
        @Schema(description = "The id of the case execution.")
        String id,

        @Schema(description = "The id of the case instance this execution belongs to.")
        String caseInstanceId,

        @Schema(description = "The id of the case definition this execution belongs to.")
        String caseDefinitionId,

        @Schema(description = "The id of the activity associated with this case execution.")
        String activityId,

        @Schema(description = "The name of the activity associated with this case execution.")
        String activityName,

        @Schema(description = "The type of the activity associated with this case execution.")
        String activityType,

        @Schema(description = "The id of the parent case execution.")
        String parentId,

        @Schema(description = "Whether the case execution is required.")
        boolean required,

        @Schema(description = "Whether the case execution is available.")
        boolean available,

        @Schema(description = "Whether the case execution is active.")
        boolean active,

        @Schema(description = "Whether the case execution is enabled.")
        boolean enabled,

        @Schema(description = "Whether the case execution is disabled.")
        boolean disabled,

        @Schema(description = "Whether the case execution is terminated.")
        boolean terminated,

        @Schema(description = "The id of the tenant this case execution belongs to.")
        String tenantId
) {
    public static CaseExecutionResultDto fromCaseExecution(CaseExecution caseExecution) {
        return new CaseExecutionResultDto(
                caseExecution.getId(),
                caseExecution.getCaseInstanceId(),
                caseExecution.getCaseDefinitionId(),
                caseExecution.getActivityId(),
                caseExecution.getActivityName(),
                caseExecution.getActivityType(),
                caseExecution.getParentId(),
                caseExecution.isRequired(),
                caseExecution.isAvailable(),
                caseExecution.isActive(),
                caseExecution.isEnabled(),
                caseExecution.isDisabled(),
                caseExecution.isTerminated(),
                caseExecution.getTenantId()
        );
    }
}

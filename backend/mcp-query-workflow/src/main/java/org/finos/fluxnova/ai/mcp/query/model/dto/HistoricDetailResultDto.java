package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricDetail;

import java.util.Date;

/**
 * DTO representing a single historic detail result.
 */
@Schema(description = "A historic detail recorded during process execution "
        + "(variable update, form property, or form field).")
public record HistoricDetailResultDto(
        @Schema(description = "The id of the historic detail.") String id,
        @Schema(description = "The key of the process definition.") String processDefinitionKey,
        @Schema(description = "The id of the process definition.") String processDefinitionId,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The id of the process instance.") String processInstanceId,
        @Schema(description = "The id of the activity instance.") String activityInstanceId,
        @Schema(description = "The id of the execution.") String executionId,
        @Schema(description = "The key of the case definition.") String caseDefinitionKey,
        @Schema(description = "The id of the case definition.") String caseDefinitionId,
        @Schema(description = "The id of the case instance.") String caseInstanceId,
        @Schema(description = "The id of the case execution.") String caseExecutionId,
        @Schema(description = "The id of the task, if applicable.") String taskId,
        @Schema(description = "The time this detail was recorded.") Date time,
        @Schema(description = "The id of the tenant this detail belongs to.") String tenantId,
        @Schema(description = "The id of the user operation that caused this detail.") String userOperationId,
        @Schema(description = "The time this historic detail will be removed.") Date removalTime
) {
    public static HistoricDetailResultDto fromHistoricDetail(HistoricDetail historicDetail) {
        return new HistoricDetailResultDto(
                historicDetail.getId(),
                historicDetail.getProcessDefinitionKey(),
                historicDetail.getProcessDefinitionId(),
                historicDetail.getRootProcessInstanceId(),
                historicDetail.getProcessInstanceId(),
                historicDetail.getActivityInstanceId(),
                historicDetail.getExecutionId(),
                historicDetail.getCaseDefinitionKey(),
                historicDetail.getCaseDefinitionId(),
                historicDetail.getCaseInstanceId(),
                historicDetail.getCaseExecutionId(),
                historicDetail.getTaskId(),
                historicDetail.getTime(),
                historicDetail.getTenantId(),
                historicDetail.getUserOperationId(),
                historicDetail.getRemovalTime()
        );
    }
}

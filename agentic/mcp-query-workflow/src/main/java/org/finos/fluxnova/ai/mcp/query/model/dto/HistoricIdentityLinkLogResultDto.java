package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricIdentityLinkLog;

import java.util.Date;

/**
 * DTO representing a single historic identity link log entry result.
 */
@Schema(description = "A historic identity link log entry.")
public record HistoricIdentityLinkLogResultDto(
        @Schema(description = "The id of the historic identity link log entry.") String id,
        @Schema(description = "The type of the identity link.") String type,
        @Schema(description = "The id of the user.") String userId,
        @Schema(description = "The id of the group.") String groupId,
        @Schema(description = "The id of the task.") String taskId,
        @Schema(description = "The id of the user who performed the operation.") String assignerId,
        @Schema(description = "The type of the operation (add or delete).") String operationType,
        @Schema(description = "The time the operation was performed.") Date time,
        @Schema(description = "The id of the process definition.") String processDefinitionId,
        @Schema(description = "The key of the process definition.") String processDefinitionKey,
        @Schema(description = "The id of the tenant.") String tenantId,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The time this historic identity link log entry will be removed.") Date removalTime
) {
    public static HistoricIdentityLinkLogResultDto fromHistoricIdentityLinkLog(HistoricIdentityLinkLog historicIdentityLinkLog) {
        return new HistoricIdentityLinkLogResultDto(
                historicIdentityLinkLog.getId(),
                historicIdentityLinkLog.getType(),
                historicIdentityLinkLog.getUserId(),
                historicIdentityLinkLog.getGroupId(),
                historicIdentityLinkLog.getTaskId(),
                historicIdentityLinkLog.getAssignerId(),
                historicIdentityLinkLog.getOperationType(),
                historicIdentityLinkLog.getTime(),
                historicIdentityLinkLog.getProcessDefinitionId(),
                historicIdentityLinkLog.getProcessDefinitionKey(),
                historicIdentityLinkLog.getTenantId(),
                historicIdentityLinkLog.getRootProcessInstanceId(),
                historicIdentityLinkLog.getRemovalTime()
        );
    }
}

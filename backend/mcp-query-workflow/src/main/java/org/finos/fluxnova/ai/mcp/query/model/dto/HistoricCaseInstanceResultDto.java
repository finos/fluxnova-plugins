package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricCaseInstance;

import java.util.Date;

/**
 * DTO representing a single historic case instance result.
 */
@Schema(description = "A historic case instance.")
public record HistoricCaseInstanceResultDto(
        @Schema(description = "The id of the historic case instance.") String id,
        @Schema(description = "The business key of the case instance.") String businessKey,
        @Schema(description = "The id of the case definition.") String caseDefinitionId,
        @Schema(description = "The key of the case definition.") String caseDefinitionKey,
        @Schema(description = "The name of the case definition.") String caseDefinitionName,
        @Schema(description = "The time the case instance was created.") Date createTime,
        @Schema(description = "The time the case instance was closed.") Date closeTime,
        @Schema(description = "The duration of the case instance in milliseconds.") Long durationInMillis,
        @Schema(description = "The id of the user who created the case instance.") String createUserId,
        @Schema(description = "The id of the super case instance.") String superCaseInstanceId,
        @Schema(description = "The id of the super process instance.") String superProcessInstanceId,
        @Schema(description = "The id of the tenant.") String tenantId,
        @Schema(description = "Whether the case instance is active.") boolean active,
        @Schema(description = "Whether the case instance is completed.") boolean completed,
        @Schema(description = "Whether the case instance is terminated.") boolean terminated,
        @Schema(description = "Whether the case instance is closed.") boolean closed
) {
    public static HistoricCaseInstanceResultDto fromHistoricCaseInstance(HistoricCaseInstance historicCaseInstance) {
        return new HistoricCaseInstanceResultDto(
                historicCaseInstance.getId(),
                historicCaseInstance.getBusinessKey(),
                historicCaseInstance.getCaseDefinitionId(),
                historicCaseInstance.getCaseDefinitionKey(),
                historicCaseInstance.getCaseDefinitionName(),
                historicCaseInstance.getCreateTime(),
                historicCaseInstance.getCloseTime(),
                historicCaseInstance.getDurationInMillis(),
                historicCaseInstance.getCreateUserId(),
                historicCaseInstance.getSuperCaseInstanceId(),
                historicCaseInstance.getSuperProcessInstanceId(),
                historicCaseInstance.getTenantId(),
                historicCaseInstance.isActive(),
                historicCaseInstance.isCompleted(),
                historicCaseInstance.isTerminated(),
                historicCaseInstance.isClosed()
        );
    }
}

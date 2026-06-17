package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.runtime.CaseInstance;

/**
 * Result DTO representing a case instance.
 */
@Schema(description = "Represents a running case instance.")
public record CaseInstanceResultDto(
        @Schema(description = "The id of the case instance.")
        String id,

        @Schema(description = "The id of the case instance (same as id for the root execution).")
        String caseInstanceId,

        @Schema(description = "The id of the case definition.")
        String caseDefinitionId,

        @Schema(description = "The business key of the case instance.")
        String businessKey,

        @Schema(description = "The id of the parent case execution, if any.")
        String parentId,

        @Schema(description = "Whether the case instance is active.")
        boolean active,

        @Schema(description = "Whether the case instance is completed.")
        boolean completed,

        @Schema(description = "Whether the case instance is terminated.")
        boolean terminated,

        @Schema(description = "The id of the tenant this case instance belongs to.")
        String tenantId
) {
    public static CaseInstanceResultDto fromCaseInstance(CaseInstance caseInstance) {
        return new CaseInstanceResultDto(
                caseInstance.getId(),
                caseInstance.getCaseInstanceId(),
                caseInstance.getCaseDefinitionId(),
                caseInstance.getBusinessKey(),
                caseInstance.getParentId(),
                caseInstance.isActive(),
                caseInstance.isCompleted(),
                caseInstance.isTerminated(),
                caseInstance.getTenantId()
        );
    }
}

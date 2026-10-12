package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricProcessInstance;

import java.util.Date;

/**
 * DTO representing a single historic process instance result.
 */
@Schema(description = "A historic process instance that has been or is being executed.")
public record HistoricProcessInstanceResultDto(
        @Schema(description = "The id of the historic process instance.") String id,
        @Schema(description = "The business key for this process instance.") String businessKey,
        @Schema(description = "The key of the process definition this instance belongs to.") String processDefinitionKey,
        @Schema(description = "The id of the process definition this instance belongs to.") String processDefinitionId,
        @Schema(description = "The name of the process definition.") String processDefinitionName,
        @Schema(description = "The version of the process definition.") Integer processDefinitionVersion,
        @Schema(description = "The time the process was started.") Date startTime,
        @Schema(description = "The time the process ended, or null if not yet ended.") Date endTime,
        @Schema(description = "The time the historic instance will be removed.") Date removalTime,
        @Schema(description = "The duration of the process instance in milliseconds.") Long durationInMillis,
        @Schema(description = "The id of the user who started the process instance.") String startUserId,
        @Schema(description = "The id of the start activity.") String startActivityId,
        @Schema(description = "The reason for deletion, if deleted.") String deleteReason,
        @Schema(description = "The id of the super process instance, if any.") String superProcessInstanceId,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The id of the super case instance, if any.") String superCaseInstanceId,
        @Schema(description = "The id of the case instance this process instance was started from.") String caseInstanceId,
        @Schema(description = "The id of the tenant this process instance belongs to.") String tenantId,
        @Schema(description = "The state of the historic process instance "
                + "(ACTIVE, SUSPENDED, COMPLETED, EXTERNALLY_TERMINATED, INTERNALLY_TERMINATED).") String state
) {
    public static HistoricProcessInstanceResultDto fromHistoricProcessInstance(HistoricProcessInstance historicProcessInstance) {
        return new HistoricProcessInstanceResultDto(
                historicProcessInstance.getId(),
                historicProcessInstance.getBusinessKey(),
                historicProcessInstance.getProcessDefinitionKey(),
                historicProcessInstance.getProcessDefinitionId(),
                historicProcessInstance.getProcessDefinitionName(),
                historicProcessInstance.getProcessDefinitionVersion(),
                historicProcessInstance.getStartTime(),
                historicProcessInstance.getEndTime(),
                historicProcessInstance.getRemovalTime(),
                historicProcessInstance.getDurationInMillis(),
                historicProcessInstance.getStartUserId(),
                historicProcessInstance.getStartActivityId(),
                historicProcessInstance.getDeleteReason(),
                historicProcessInstance.getSuperProcessInstanceId(),
                historicProcessInstance.getRootProcessInstanceId(),
                historicProcessInstance.getSuperCaseInstanceId(),
                historicProcessInstance.getCaseInstanceId(),
                historicProcessInstance.getTenantId(),
                historicProcessInstance.getState()
        );
    }
}

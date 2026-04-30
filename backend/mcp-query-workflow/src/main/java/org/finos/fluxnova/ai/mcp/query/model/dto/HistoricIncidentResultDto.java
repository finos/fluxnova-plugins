package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricIncident;

import java.util.Date;

/**
 * DTO representing a single historic incident result.
 */
@Schema(description = "A historic incident.")
public record HistoricIncidentResultDto(
        @Schema(description = "The id of the historic incident.") String id,
        @Schema(description = "The time the incident was created.") Date createTime,
        @Schema(description = "The time the incident was resolved or deleted.") Date endTime,
        @Schema(description = "The type of the incident.") String incidentType,
        @Schema(description = "The message of the incident.") String incidentMessage,
        @Schema(description = "The id of the execution.") String executionId,
        @Schema(description = "The id of the activity where the incident occurred.") String activityId,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The id of the process instance.") String processInstanceId,
        @Schema(description = "The id of the process definition.") String processDefinitionId,
        @Schema(description = "The key of the process definition.") String processDefinitionKey,
        @Schema(description = "The id of the incident that caused this incident.") String causeIncidentId,
        @Schema(description = "The id of the root cause incident.") String rootCauseIncidentId,
        @Schema(description = "The configuration of the incident.") String configuration,
        @Schema(description = "The history configuration of the incident.") String historyConfiguration,
        @Schema(description = "Whether the incident is open.") boolean open,
        @Schema(description = "Whether the incident has been deleted.") boolean deleted,
        @Schema(description = "Whether the incident has been resolved.") boolean resolved,
        @Schema(description = "The id of the tenant.") String tenantId,
        @Schema(description = "The id of the job definition.") String jobDefinitionId,
        @Schema(description = "The time this historic incident will be removed.") Date removalTime,
        @Schema(description = "The id of the activity where the failing action occurred.") String failedActivityId,
        @Schema(description = "An annotation added to this historic incident.") String annotation
) {
    public static HistoricIncidentResultDto fromHistoricIncident(HistoricIncident historicIncident) {
        return new HistoricIncidentResultDto(
                historicIncident.getId(),
                historicIncident.getCreateTime(),
                historicIncident.getEndTime(),
                historicIncident.getIncidentType(),
                historicIncident.getIncidentMessage(),
                historicIncident.getExecutionId(),
                historicIncident.getActivityId(),
                historicIncident.getRootProcessInstanceId(),
                historicIncident.getProcessInstanceId(),
                historicIncident.getProcessDefinitionId(),
                historicIncident.getProcessDefinitionKey(),
                historicIncident.getCauseIncidentId(),
                historicIncident.getRootCauseIncidentId(),
                historicIncident.getConfiguration(),
                historicIncident.getHistoryConfiguration(),
                historicIncident.isOpen(),
                historicIncident.isDeleted(),
                historicIncident.isResolved(),
                historicIncident.getTenantId(),
                historicIncident.getJobDefinitionId(),
                historicIncident.getRemovalTime(),
                historicIncident.getFailedActivityId(),
                historicIncident.getAnnotation()
        );
    }
}

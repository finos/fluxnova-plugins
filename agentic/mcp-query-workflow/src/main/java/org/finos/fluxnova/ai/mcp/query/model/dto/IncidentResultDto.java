package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.runtime.Incident;

import java.util.Date;

/**
 * Result DTO for incident query results.
 * Maps the fields from the engine's {@link Incident} interface.
 */
public record IncidentResultDto(
        String id,
        Date incidentTimestamp,
        String incidentType,
        String incidentMessage,
        String executionId,
        String activityId,
        String failedActivityId,
        String processInstanceId,
        String processDefinitionId,
        String causeIncidentId,
        String rootCauseIncidentId,
        String configuration,
        String tenantId,
        String jobDefinitionId,
        String annotation
) {
    public static IncidentResultDto fromIncident(Incident incident) {
        return new IncidentResultDto(
                incident.getId(),
                incident.getIncidentTimestamp(),
                incident.getIncidentType(),
                incident.getIncidentMessage(),
                incident.getExecutionId(),
                incident.getActivityId(),
                incident.getFailedActivityId(),
                incident.getProcessInstanceId(),
                incident.getProcessDefinitionId(),
                incident.getCauseIncidentId(),
                incident.getRootCauseIncidentId(),
                incident.getConfiguration(),
                incident.getTenantId(),
                incident.getJobDefinitionId(),
                incident.getAnnotation()
        );
    }
}

package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.IncidentQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying incidents via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering incidents.")
public record IncidentQueryDto(
        @Schema(description = "Restricts to incidents that have the given id.")
        String incidentId,

        @Schema(description = "Filter by incident type.", allowableValues = {"failedJob", "failedExternalTask"})
        String incidentType,

        @Schema(description = "Restricts to incidents that have the given incident message.")
        String incidentMessage,

        @Schema(description = "Restricts to incidents that have an incident message that is a substring of the given value. "
                + "The string can include the wildcard character '%' to express like-strategy: "
                + "starts with (string%), ends with (%string) or contains (%string%).")
        String incidentMessageLike,

        @Schema(description = "Restricts to incidents that belong to a process definition with the given id.")
        String processDefinitionId,

        @Schema(description = "Restricts to incidents that belong to a process definition with one of the given keys.")
        List<String> processDefinitionKeyIn,

        @Schema(description = "Restricts to incidents that belong to a process instance with the given id.")
        String processInstanceId,

        @Schema(description = "Restricts to incidents that belong to an execution with the given id.")
        String executionId,

        @Schema(description = "Restricts to incidents that have an incidentTimestamp date before the given date. "
                + "By default, the date must be in the format yyyy-MM-dd'T'HH:mm:ss.SSSZ.")
        Date incidentTimestampBefore,

        @Schema(description = "Restricts to incidents that have an incidentTimestamp date after the given date. "
                + "By default, the date must be in the format yyyy-MM-dd'T'HH:mm:ss.SSSZ.")
        Date incidentTimestampAfter,

        @Schema(description = "Restricts to incidents that belong to an activity with the given id.")
        String activityId,

        @Schema(description = "Restricts to incidents that were created due to the failure of an activity with the given id.")
        String failedActivityId,

        @Schema(description = "Restricts to incidents that have the given incident id as cause incident.")
        String causeIncidentId,

        @Schema(description = "Restricts to incidents that have the given incident id as root cause incident.")
        String rootCauseIncidentId,

        @Schema(description = "Restricts to incidents that have the given parameter set as configuration.")
        String configuration,

        @Schema(description = "Restricts to incidents that have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Restricts to incidents that have one of the given job definition ids.")
        List<String> jobDefinitionIdIn
) {
    public IncidentQuery toQuery(RuntimeService runtimeService) {
        IncidentQuery query = runtimeService.createIncidentQuery();
        if (incidentId != null) {
            query.incidentId(incidentId);
        }
        if (incidentType != null) {
            query.incidentType(incidentType);
        }
        if (incidentMessage != null) {
            query.incidentMessage(incidentMessage);
        }
        if (incidentMessageLike != null) {
            query.incidentMessageLike(incidentMessageLike);
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (processDefinitionKeyIn != null && !processDefinitionKeyIn.isEmpty()) {
            query.processDefinitionKeyIn(processDefinitionKeyIn.toArray(new String[0]));
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (incidentTimestampBefore != null) {
            query.incidentTimestampBefore(incidentTimestampBefore);
        }
        if (incidentTimestampAfter != null) {
            query.incidentTimestampAfter(incidentTimestampAfter);
        }
        if (activityId != null) {
            query.activityId(activityId);
        }
        if (failedActivityId != null) {
            query.failedActivityId(failedActivityId);
        }
        if (causeIncidentId != null) {
            query.causeIncidentId(causeIncidentId);
        }
        if (rootCauseIncidentId != null) {
            query.rootCauseIncidentId(rootCauseIncidentId);
        }
        if (configuration != null) {
            query.configuration(configuration);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (jobDefinitionIdIn != null && !jobDefinitionIdIn.isEmpty()) {
            query.jobDefinitionIdIn(jobDefinitionIdIn.toArray(new String[0]));
        }
        return query;
    }
}

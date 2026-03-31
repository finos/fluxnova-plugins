package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricIncidentQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying historic incidents.
 */
@Schema(description = "Parameters for querying historic incidents.")
public record HistoricIncidentQueryDto(
        @Schema(description = "Filter by the id of the historic incident.") String incidentId,
        @Schema(description = "Filter by the type of the incident.") String incidentType,
        @Schema(description = "Filter by the message of the incident.") String incidentMessage,
        @Schema(description = "Filter by a partial match on the incident message (% wildcard supported).") String incidentMessageLike,
        @Schema(description = "Filter by the id of the process definition.") String processDefinitionId,
        @Schema(description = "Filter by the key of the process definition.") String processDefinitionKey,
        @Schema(description = "Filter by a list of process definition keys.") List<String> processDefinitionKeyIn,
        @Schema(description = "Filter by the id of the process instance.") String processInstanceId,
        @Schema(description = "Filter by the id of the execution.") String executionId,
        @Schema(description = "Only return incidents created before this date.") Date createTimeBefore,
        @Schema(description = "Only return incidents created after this date.") Date createTimeAfter,
        @Schema(description = "Only return incidents that ended before this date.") Date endTimeBefore,
        @Schema(description = "Only return incidents that ended after this date.") Date endTimeAfter,
        @Schema(description = "Filter by the activity id where the incident occurred.") String activityId,
        @Schema(description = "Filter by the activity id where the action that failed occurred.") String failedActivityId,
        @Schema(description = "Filter by the id of the incident that caused this incident.") String causeIncidentId,
        @Schema(description = "Filter by the id of the incident that is the root cause of this incident.") String rootCauseIncidentId,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only incidents without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Filter by the configuration of the incident.") String configuration,
        @Schema(description = "Filter by the history configuration of the incident.") String historyConfiguration,
        @Schema(description = "Filter by a list of job definition ids.") List<String> jobDefinitionIdIn,
        @Schema(description = "If true, only open incidents are returned.") Boolean open,
        @Schema(description = "If true, only resolved incidents are returned.") Boolean resolved,
        @Schema(description = "If true, only deleted incidents are returned.") Boolean deleted,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricIncidentQuery toQuery(HistoricIncidentQuery query) {
        if (incidentId != null) query.incidentId(incidentId);
        if (incidentType != null) query.incidentType(incidentType);
        if (incidentMessage != null) query.incidentMessage(incidentMessage);
        if (incidentMessageLike != null) query.incidentMessageLike(incidentMessageLike);
        if (processDefinitionId != null) query.processDefinitionId(processDefinitionId);
        if (processDefinitionKey != null) query.processDefinitionKey(processDefinitionKey);
        if (processDefinitionKeyIn != null && !processDefinitionKeyIn.isEmpty())
            query.processDefinitionKeyIn(processDefinitionKeyIn.toArray(new String[0]));
        if (processInstanceId != null) query.processInstanceId(processInstanceId);
        if (executionId != null) query.executionId(executionId);
        if (createTimeBefore != null) query.createTimeBefore(createTimeBefore);
        if (createTimeAfter != null) query.createTimeAfter(createTimeAfter);
        if (endTimeBefore != null) query.endTimeBefore(endTimeBefore);
        if (endTimeAfter != null) query.endTimeAfter(endTimeAfter);
        if (activityId != null) query.activityId(activityId);
        if (failedActivityId != null) query.failedActivityId(failedActivityId);
        if (causeIncidentId != null) query.causeIncidentId(causeIncidentId);
        if (rootCauseIncidentId != null) query.rootCauseIncidentId(rootCauseIncidentId);
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        if (configuration != null) query.configuration(configuration);
        if (historyConfiguration != null) query.historyConfiguration(historyConfiguration);
        if (jobDefinitionIdIn != null && !jobDefinitionIdIn.isEmpty())
            query.jobDefinitionIdIn(jobDefinitionIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(open)) query.open();
        if (Boolean.TRUE.equals(resolved)) query.resolved();
        if (Boolean.TRUE.equals(deleted)) query.deleted();
        return query;
    }
}

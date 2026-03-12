package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricDecisionInstanceQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying historic decision instances.
 */
@Schema(description = "Parameters for querying historic decision instances.")
public record HistoricDecisionInstanceQueryDto(
        @Schema(description = "Filter by the id of the historic decision instance.") String decisionInstanceId,
        @Schema(description = "Filter by a list of historic decision instance ids.") List<String> decisionInstanceIdIn,
        @Schema(description = "Filter by the id of the decision definition.") String decisionDefinitionId,
        @Schema(description = "Filter by a list of decision definition ids.") List<String> decisionDefinitionIdIn,
        @Schema(description = "Filter by the key of the decision definition.") String decisionDefinitionKey,
        @Schema(description = "Filter by a list of decision definition keys.") List<String> decisionDefinitionKeyIn,
        @Schema(description = "Filter by the name of the decision definition.") String decisionDefinitionName,
        @Schema(description = "Filter by a partial match on the decision definition name (% wildcard supported).") String decisionDefinitionNameLike,
        @Schema(description = "Filter by the key of the process definition.") String processDefinitionKey,
        @Schema(description = "Filter by the id of the process definition.") String processDefinitionId,
        @Schema(description = "Filter by the id of the process instance.") String processInstanceId,
        @Schema(description = "Filter by the key of the case definition.") String caseDefinitionKey,
        @Schema(description = "Filter by the id of the case definition.") String caseDefinitionId,
        @Schema(description = "Filter by the id of the case instance.") String caseInstanceId,
        @Schema(description = "Filter by a list of activity ids.") List<String> activityIdIn,
        @Schema(description = "Filter by a list of activity instance ids.") List<String> activityInstanceIdIn,
        @Schema(description = "Only return instances evaluated before this date.") Date evaluatedBefore,
        @Schema(description = "Only return instances evaluated after this date.") Date evaluatedAfter,
        @Schema(description = "Filter by the id of the user who evaluated the decision.") String userId,
        @Schema(description = "If true, inputs are included in the results.") Boolean includeInputs,
        @Schema(description = "If true, outputs are included in the results.") Boolean includeOutputs,
        @Schema(description = "Filter by the id of the root decision instance.") String rootDecisionInstanceId,
        @Schema(description = "If true, only root decision instances are returned.") Boolean rootDecisionInstancesOnly,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only instances without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricDecisionInstanceQuery toQuery(HistoricDecisionInstanceQuery query) {
        if (decisionInstanceId != null) query.decisionInstanceId(decisionInstanceId);
        if (decisionInstanceIdIn != null && !decisionInstanceIdIn.isEmpty())
            query.decisionInstanceIdIn(decisionInstanceIdIn.toArray(new String[0]));
        if (decisionDefinitionId != null) query.decisionDefinitionId(decisionDefinitionId);
        if (decisionDefinitionIdIn != null && !decisionDefinitionIdIn.isEmpty())
            query.decisionDefinitionIdIn(decisionDefinitionIdIn.toArray(new String[0]));
        if (decisionDefinitionKey != null) query.decisionDefinitionKey(decisionDefinitionKey);
        if (decisionDefinitionKeyIn != null && !decisionDefinitionKeyIn.isEmpty())
            query.decisionDefinitionKeyIn(decisionDefinitionKeyIn.toArray(new String[0]));
        if (decisionDefinitionName != null) query.decisionDefinitionName(decisionDefinitionName);
        if (decisionDefinitionNameLike != null) query.decisionDefinitionNameLike(decisionDefinitionNameLike);
        if (processDefinitionKey != null) query.processDefinitionKey(processDefinitionKey);
        if (processDefinitionId != null) query.processDefinitionId(processDefinitionId);
        if (processInstanceId != null) query.processInstanceId(processInstanceId);
        if (caseDefinitionKey != null) query.caseDefinitionKey(caseDefinitionKey);
        if (caseDefinitionId != null) query.caseDefinitionId(caseDefinitionId);
        if (caseInstanceId != null) query.caseInstanceId(caseInstanceId);
        if (activityIdIn != null && !activityIdIn.isEmpty())
            query.activityIdIn(activityIdIn.toArray(new String[0]));
        if (activityInstanceIdIn != null && !activityInstanceIdIn.isEmpty())
            query.activityInstanceIdIn(activityInstanceIdIn.toArray(new String[0]));
        if (evaluatedBefore != null) query.evaluatedBefore(evaluatedBefore);
        if (evaluatedAfter != null) query.evaluatedAfter(evaluatedAfter);
        if (userId != null) query.userId(userId);
        if (Boolean.TRUE.equals(includeInputs)) query.includeInputs();
        if (Boolean.TRUE.equals(includeOutputs)) query.includeOutputs();
        if (rootDecisionInstanceId != null) query.rootDecisionInstanceId(rootDecisionInstanceId);
        if (Boolean.TRUE.equals(rootDecisionInstancesOnly)) query.rootDecisionInstancesOnly();
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        return query;
    }
}

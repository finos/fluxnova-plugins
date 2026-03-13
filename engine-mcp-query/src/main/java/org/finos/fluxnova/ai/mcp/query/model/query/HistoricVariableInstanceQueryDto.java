package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricVariableInstanceQuery;

import java.util.List;

/**
 * DTO for querying historic variable instances.
 */
@Schema(description = "Parameters for querying historic variable instances.")
public record HistoricVariableInstanceQueryDto(
        @Schema(description = "Filter by the id of the historic variable instance.") String variableId,
        @Schema(description = "Filter by the id of the process instance the variable belongs to.") String processInstanceId,
        @Schema(description = "Filter by the id of the process definition the variable belongs to.") String processDefinitionId,
        @Schema(description = "Filter by the key of the process definition the variable belongs to.") String processDefinitionKey,
        @Schema(description = "Filter by the id of the case instance the variable belongs to.") String caseInstanceId,
        @Schema(description = "Filter by the name of the variable.") String variableName,
        @Schema(description = "Filter by a partial match on the variable name (% wildcard supported).") String variableNameLike,
        @Schema(description = "Filter by variable type names (e.g. String, Integer, Boolean).") List<String> variableTypeIn,
        @Schema(description = "If true, variable name comparisons are case-insensitive.") Boolean matchVariableNamesIgnoreCase,
        @Schema(description = "If true, variable value comparisons are case-insensitive.") Boolean matchVariableValuesIgnoreCase,
        @Schema(description = "Filter by a list of process instance ids.") List<String> processInstanceIdIn,
        @Schema(description = "Filter by a list of task ids.") List<String> taskIdIn,
        @Schema(description = "Filter by a list of execution ids.") List<String> executionIdIn,
        @Schema(description = "Filter by a list of case execution ids.") List<String> caseExecutionIdIn,
        @Schema(description = "Filter by a list of case activity ids.") List<String> caseActivityIdIn,
        @Schema(description = "Filter by a list of activity instance ids.") List<String> activityInstanceIdIn,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only instances without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricVariableInstanceQuery toQuery(HistoricVariableInstanceQuery query) {
        if (variableId != null) query.variableId(variableId);
        if (processInstanceId != null) query.processInstanceId(processInstanceId);
        if (processDefinitionId != null) query.processDefinitionId(processDefinitionId);
        if (processDefinitionKey != null) query.processDefinitionKey(processDefinitionKey);
        if (caseInstanceId != null) query.caseInstanceId(caseInstanceId);
        if (variableName != null) query.variableName(variableName);
        if (variableNameLike != null) query.variableNameLike(variableNameLike);
        if (variableTypeIn != null && !variableTypeIn.isEmpty())
            query.variableTypeIn(variableTypeIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(matchVariableNamesIgnoreCase)) query.matchVariableNamesIgnoreCase();
        if (Boolean.TRUE.equals(matchVariableValuesIgnoreCase)) query.matchVariableValuesIgnoreCase();
        if (processInstanceIdIn != null && !processInstanceIdIn.isEmpty())
            query.processInstanceIdIn(processInstanceIdIn.toArray(new String[0]));
        if (taskIdIn != null && !taskIdIn.isEmpty())
            query.taskIdIn(taskIdIn.toArray(new String[0]));
        if (executionIdIn != null && !executionIdIn.isEmpty())
            query.executionIdIn(executionIdIn.toArray(new String[0]));
        if (caseExecutionIdIn != null && !caseExecutionIdIn.isEmpty())
            query.caseExecutionIdIn(caseExecutionIdIn.toArray(new String[0]));
        if (caseActivityIdIn != null && !caseActivityIdIn.isEmpty())
            query.caseActivityIdIn(caseActivityIdIn.toArray(new String[0]));
        if (activityInstanceIdIn != null && !activityInstanceIdIn.isEmpty())
            query.activityInstanceIdIn(activityInstanceIdIn.toArray(new String[0]));
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        return query;
    }
}

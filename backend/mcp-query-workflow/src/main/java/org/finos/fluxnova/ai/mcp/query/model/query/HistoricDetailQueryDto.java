package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.HistoryService;
import org.finos.fluxnova.bpm.engine.history.HistoricDetailQuery;

import java.util.List;

/**
 * DTO for querying historic details via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering historic details "
        + "(variable updates and form fields recorded during process execution).")
public record HistoricDetailQueryDto(
        @Schema(description = "Filter by the id of the historic detail.")
        String detailId,

        @Schema(description = "Filter by the id of the process instance.")
        String processInstanceId,

        @Schema(description = "Filter by the id of the case instance.")
        String caseInstanceId,

        @Schema(description = "Filter by the id of the execution.")
        String executionId,

        @Schema(description = "Filter by the id of the case execution.")
        String caseExecutionId,

        @Schema(description = "Filter by the id of the activity instance.")
        String activityInstanceId,

        @Schema(description = "Filter by the id of the task.")
        String taskId,

        @Schema(description = "Filter by the id of the variable instance.")
        String variableInstanceId,

        @Schema(description = "Filter by a list of variable types (e.g. String, Integer, Boolean).")
        List<String> variableTypeIn,

        @Schema(description = "Filter by a variable name pattern (use % as wildcard).")
        String variableNameLike,

        @Schema(description = "Only include historic form fields.")
        Boolean formFields,

        @Schema(description = "Only include variable updates.")
        Boolean variableUpdates,

        @Schema(description = "Exclude details that originate from tasks.")
        Boolean excludeTaskDetails,

        @Schema(description = "Filter by the id of the user operation that caused this detail.")
        String userOperationId,

        @Schema(description = "Filter by a list of process instance ids.")
        List<String> processInstanceIdIn,

        @Schema(description = "Filter by a list of tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include details which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Maximum number of results to return.")
        Integer maxResults
) {
    public HistoricDetailQuery toQuery(HistoryService historyService) {
        HistoricDetailQuery query = historyService.createHistoricDetailQuery();
        if (detailId != null) {
            query.detailId(detailId);
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (caseInstanceId != null) {
            query.caseInstanceId(caseInstanceId);
        }
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (caseExecutionId != null) {
            query.caseExecutionId(caseExecutionId);
        }
        if (activityInstanceId != null) {
            query.activityInstanceId(activityInstanceId);
        }
        if (taskId != null) {
            query.taskId(taskId);
        }
        if (variableInstanceId != null) {
            query.variableInstanceId(variableInstanceId);
        }
        if (variableTypeIn != null && !variableTypeIn.isEmpty()) {
            query.variableTypeIn(variableTypeIn.toArray(new String[0]));
        }
        if (variableNameLike != null) {
            query.variableNameLike(variableNameLike);
        }
        if (Boolean.TRUE.equals(formFields)) {
            query.formFields();
        }
        if (Boolean.TRUE.equals(variableUpdates)) {
            query.variableUpdates();
        }
        if (Boolean.TRUE.equals(excludeTaskDetails)) {
            query.excludeTaskDetails();
        }
        if (userOperationId != null) {
            query.userOperationId(userOperationId);
        }
        if (processInstanceIdIn != null && !processInstanceIdIn.isEmpty()) {
            query.processInstanceIdIn(processInstanceIdIn.toArray(new String[0]));
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        return query;
    }
}

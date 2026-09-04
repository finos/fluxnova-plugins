package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.VariableInstanceQuery;

import java.util.List;

/**
 * DTO for querying variable instances via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering variable instances.")
public record VariableInstanceQueryDto(
        @Schema(description = "Filter by variable instance name.")
        String variableName,

        @Schema(description = "Filter by the variable instance name. The string can include the wildcard character '%' "
                + "to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).")
        String variableNameLike,

        @Schema(description = "Only include variable instances which belong to one of the passed process instance ids.")
        List<String> processInstanceIdIn,

        @Schema(description = "Only include variable instances which belong to one of the passed execution ids.")
        List<String> executionIdIn,

        @Schema(description = "Only include variable instances which belong to one of the passed case instance ids.")
        List<String> caseInstanceIdIn,

        @Schema(description = "Only include variable instances which belong to one of the passed case execution ids.")
        List<String> caseExecutionIdIn,

        @Schema(description = "Only include variable instances which belong to one of the passed task ids.")
        List<String> taskIdIn,

        @Schema(description = "Only include variable instances which are related to one of the passed batch ids.")
        List<String> batchIdIn,

        @Schema(description = "Only include variable instances which belong to one of the passed activity instance ids.")
        List<String> activityInstanceIdIn,

        @Schema(description = "Only include variable instances which belong to one of the passed tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include variable instances which have one of the passed variable names.")
        List<String> variableNameIn,

        @Schema(description = "Only include variable instances which belong to one of the passed scope ids.")
        List<String> variableScopeIdIn,

        @Schema(description = "Match all variable names case-insensitively when used with variable value filters.")
        Boolean variableNamesIgnoreCase,

        @Schema(description = "Match all variable values case-insensitively when used with variable value filters.")
        Boolean variableValuesIgnoreCase
) {
    public VariableInstanceQuery toQuery(RuntimeService runtimeService) {
        VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();
        if (variableName != null) {
            query.variableName(variableName);
        }
        if (variableNameLike != null) {
            query.variableNameLike(variableNameLike);
        }
        if (variableNameIn != null && !variableNameIn.isEmpty()) {
            query.variableNameIn(variableNameIn.toArray(new String[0]));
        }
        if (processInstanceIdIn != null && !processInstanceIdIn.isEmpty()) {
            query.processInstanceIdIn(processInstanceIdIn.toArray(new String[0]));
        }
        if (executionIdIn != null && !executionIdIn.isEmpty()) {
            query.executionIdIn(executionIdIn.toArray(new String[0]));
        }
        if (caseInstanceIdIn != null && !caseInstanceIdIn.isEmpty()) {
            query.caseInstanceIdIn(caseInstanceIdIn.toArray(new String[0]));
        }
        if (caseExecutionIdIn != null && !caseExecutionIdIn.isEmpty()) {
            query.caseExecutionIdIn(caseExecutionIdIn.toArray(new String[0]));
        }
        if (taskIdIn != null && !taskIdIn.isEmpty()) {
            query.taskIdIn(taskIdIn.toArray(new String[0]));
        }
        if (batchIdIn != null && !batchIdIn.isEmpty()) {
            query.batchIdIn(batchIdIn.toArray(new String[0]));
        }
        if (activityInstanceIdIn != null && !activityInstanceIdIn.isEmpty()) {
            query.activityInstanceIdIn(activityInstanceIdIn.toArray(new String[0]));
        }
        if (variableScopeIdIn != null && !variableScopeIdIn.isEmpty()) {
            query.variableScopeIdIn(variableScopeIdIn.toArray(new String[0]));
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(variableNamesIgnoreCase)) {
            query.matchVariableNamesIgnoreCase();
        }
        if (Boolean.TRUE.equals(variableValuesIgnoreCase)) {
            query.matchVariableValuesIgnoreCase();
        }
        return query;
    }
}

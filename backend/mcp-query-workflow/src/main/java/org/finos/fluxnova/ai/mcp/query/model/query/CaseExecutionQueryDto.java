package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.CaseService;
import org.finos.fluxnova.bpm.engine.runtime.CaseExecutionQuery;

import java.util.List;

/**
 * DTO for querying case executions via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering case executions.")
public record CaseExecutionQueryDto(
        @Schema(description = "Filter by the id of the case execution.")
        String caseExecutionId,

        @Schema(description = "Filter by the id of the case instance the execution belongs to.")
        String caseInstanceId,

        @Schema(description = "Filter by case instances that have the given business key.")
        String caseInstanceBusinessKey,

        @Schema(description = "Filter by the id of the case definition the execution belongs to.")
        String caseDefinitionId,

        @Schema(description = "Filter by the key of the case definition the execution belongs to.")
        String caseDefinitionKey,

        @Schema(description = "Filter by the id of the activity the execution is currently at.")
        String activityId,

        @Schema(description = "Only include case executions that are required. "
                + "Value may only be true, as false is the default behavior.")
        Boolean required,

        @Schema(description = "Only include case executions that are available. "
                + "Value may only be true, as false is the default behavior.")
        Boolean available,

        @Schema(description = "Only include case executions that are enabled. "
                + "Value may only be true, as false is the default behavior.")
        Boolean enabled,

        @Schema(description = "Only include case executions that are active. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include case executions that are disabled. "
                + "Value may only be true, as false is the default behavior.")
        Boolean disabled,

        @Schema(description = "Filter by a list of tenant ids. A case execution must belong to one of the given tenants.")
        List<String> tenantIdIn,

        @Schema(description = "Only include case executions which belong to no tenant.")
        Boolean withoutTenantId
) {
    public CaseExecutionQuery toQuery(CaseService caseService) {
        CaseExecutionQuery query = caseService.createCaseExecutionQuery();
        if (caseExecutionId != null) {
            query.caseExecutionId(caseExecutionId);
        }
        if (caseInstanceId != null) {
            query.caseInstanceId(caseInstanceId);
        }
        if (caseInstanceBusinessKey != null) {
            query.caseInstanceBusinessKey(caseInstanceBusinessKey);
        }
        if (caseDefinitionId != null) {
            query.caseDefinitionId(caseDefinitionId);
        }
        if (caseDefinitionKey != null) {
            query.caseDefinitionKey(caseDefinitionKey);
        }
        if (activityId != null) {
            query.activityId(activityId);
        }
        if (Boolean.TRUE.equals(required)) {
            query.required();
        }
        if (Boolean.TRUE.equals(available)) {
            query.available();
        }
        if (Boolean.TRUE.equals(enabled)) {
            query.enabled();
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(disabled)) {
            query.disabled();
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

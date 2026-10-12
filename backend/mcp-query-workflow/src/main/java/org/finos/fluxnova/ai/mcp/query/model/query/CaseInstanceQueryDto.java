package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.CaseService;
import org.finos.fluxnova.bpm.engine.runtime.CaseInstanceQuery;

import java.util.List;

/**
 * DTO for querying case instances via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering case instances.")
public record CaseInstanceQueryDto(
        @Schema(description = "Filter by the id of the case instance.")
        String caseInstanceId,

        @Schema(description = "Filter by the business key of the case instance.")
        String caseInstanceBusinessKey,

        @Schema(description = "Filter by the key of the case definition the case instance belongs to.")
        String caseDefinitionKey,

        @Schema(description = "Filter by the id of the case definition the case instance belongs to.")
        String caseDefinitionId,

        @Schema(description = "Filter by the id of the deployment the case instance belongs to.")
        String deploymentId,

        @Schema(description = "Filter by the id of the super process instance "
                + "which initiated the case instance.")
        String superProcessInstanceId,

        @Schema(description = "Filter by the id of the sub process instance "
                + "which was initiated by the case instance.")
        String subProcessInstanceId,

        @Schema(description = "Filter by the id of the super case instance "
                + "which initiated the case instance.")
        String superCaseInstanceId,

        @Schema(description = "Filter by the id of the sub case instance "
                + "which was initiated by the case instance.")
        String subCaseInstanceId,

        @Schema(description = "Only include active case instances. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include completed case instances. "
                + "Value may only be true, as false is the default behavior.")
        Boolean completed,

        @Schema(description = "Only include terminated case instances. "
                + "Value may only be true, as false is the default behavior.")
        Boolean terminated,

        @Schema(description = "Filter by a list of tenant ids. A case instance must belong to one of the given tenants.")
        List<String> tenantIdIn,

        @Schema(description = "Only include case instances which belong to no tenant.")
        Boolean withoutTenantId
) {
    public CaseInstanceQuery toQuery(CaseService caseService) {
        CaseInstanceQuery query = caseService.createCaseInstanceQuery();
        if (caseInstanceId != null) {
            query.caseInstanceId(caseInstanceId);
        }
        if (caseInstanceBusinessKey != null) {
            query.caseInstanceBusinessKey(caseInstanceBusinessKey);
        }
        if (caseDefinitionKey != null) {
            query.caseDefinitionKey(caseDefinitionKey);
        }
        if (caseDefinitionId != null) {
            query.caseDefinitionId(caseDefinitionId);
        }
        if (deploymentId != null) {
            query.deploymentId(deploymentId);
        }
        if (superProcessInstanceId != null) {
            query.superProcessInstanceId(superProcessInstanceId);
        }
        if (subProcessInstanceId != null) {
            query.subProcessInstanceId(subProcessInstanceId);
        }
        if (superCaseInstanceId != null) {
            query.superCaseInstanceId(superCaseInstanceId);
        }
        if (subCaseInstanceId != null) {
            query.subCaseInstanceId(subCaseInstanceId);
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(completed)) {
            query.completed();
        }
        if (Boolean.TRUE.equals(terminated)) {
            query.terminated();
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

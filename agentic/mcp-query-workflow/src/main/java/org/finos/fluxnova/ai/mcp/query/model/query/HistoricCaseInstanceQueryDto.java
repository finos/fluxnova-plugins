package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricCaseInstanceQuery;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * DTO for querying historic case instances.
 */
@Schema(description = "Parameters for querying historic case instances.")
public record HistoricCaseInstanceQueryDto(
        @Schema(description = "Filter by the id of the case instance.") String caseInstanceId,
        @Schema(description = "Filter by a list of case instance ids.") List<String> caseInstanceIds,
        @Schema(description = "Filter by the id of the case definition.") String caseDefinitionId,
        @Schema(description = "Filter by the key of the case definition.") String caseDefinitionKey,
        @Schema(description = "Exclude case instances with these case definition keys.") List<String> caseDefinitionKeyNotIn,
        @Schema(description = "Filter by the name of the case definition.") String caseDefinitionName,
        @Schema(description = "Filter by a partial match on the case definition name (% wildcard supported).") String caseDefinitionNameLike,
        @Schema(description = "Filter by the business key of the case instance.") String caseInstanceBusinessKey,
        @Schema(description = "Filter by a partial match on the case instance business key (% wildcard supported).") String caseInstanceBusinessKeyLike,
        @Schema(description = "Filter by a list of case activity ids.") List<String> caseActivityIdIn,
        @Schema(description = "Only return instances created before this date.") Date createdBefore,
        @Schema(description = "Only return instances created after this date.") Date createdAfter,
        @Schema(description = "Only return instances closed before this date.") Date closedBefore,
        @Schema(description = "Only return instances closed after this date.") Date closedAfter,
        @Schema(description = "Filter by the id of the user who created the case instance.") String createdBy,
        @Schema(description = "Filter by the id of the super case instance.") String superCaseInstanceId,
        @Schema(description = "Filter by the id of the sub case instance.") String subCaseInstanceId,
        @Schema(description = "Filter by the id of the super process instance.") String superProcessInstanceId,
        @Schema(description = "Filter by the id of the sub process instance.") String subProcessInstanceId,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only instances without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "If true, only active case instances are returned.") Boolean active,
        @Schema(description = "If true, only completed case instances are returned.") Boolean completed,
        @Schema(description = "If true, only terminated case instances are returned.") Boolean terminated,
        @Schema(description = "If true, only closed case instances are returned.") Boolean closed,
        @Schema(description = "If true, only instances that are not closed are returned.") Boolean notClosed,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricCaseInstanceQuery toQuery(HistoricCaseInstanceQuery query) {
        if (caseInstanceId != null) query.caseInstanceId(caseInstanceId);
        if (caseInstanceIds != null && !caseInstanceIds.isEmpty())
            query.caseInstanceIds(new HashSet<>(caseInstanceIds));
        if (caseDefinitionId != null) query.caseDefinitionId(caseDefinitionId);
        if (caseDefinitionKey != null) query.caseDefinitionKey(caseDefinitionKey);
        if (caseDefinitionKeyNotIn != null && !caseDefinitionKeyNotIn.isEmpty())
            query.caseDefinitionKeyNotIn(caseDefinitionKeyNotIn);
        if (caseDefinitionName != null) query.caseDefinitionName(caseDefinitionName);
        if (caseDefinitionNameLike != null) query.caseDefinitionNameLike(caseDefinitionNameLike);
        if (caseInstanceBusinessKey != null) query.caseInstanceBusinessKey(caseInstanceBusinessKey);
        if (caseInstanceBusinessKeyLike != null) query.caseInstanceBusinessKeyLike(caseInstanceBusinessKeyLike);
        if (caseActivityIdIn != null && !caseActivityIdIn.isEmpty())
            query.caseActivityIdIn(caseActivityIdIn.toArray(new String[0]));
        if (createdBefore != null) query.createdBefore(createdBefore);
        if (createdAfter != null) query.createdAfter(createdAfter);
        if (closedBefore != null) query.closedBefore(closedBefore);
        if (closedAfter != null) query.closedAfter(closedAfter);
        if (createdBy != null) query.createdBy(createdBy);
        if (superCaseInstanceId != null) query.superCaseInstanceId(superCaseInstanceId);
        if (subCaseInstanceId != null) query.subCaseInstanceId(subCaseInstanceId);
        if (superProcessInstanceId != null) query.superProcessInstanceId(superProcessInstanceId);
        if (subProcessInstanceId != null) query.subProcessInstanceId(subProcessInstanceId);
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        if (Boolean.TRUE.equals(active)) query.active();
        if (Boolean.TRUE.equals(completed)) query.completed();
        if (Boolean.TRUE.equals(terminated)) query.terminated();
        if (Boolean.TRUE.equals(closed)) query.closed();
        if (Boolean.TRUE.equals(notClosed)) query.notClosed();
        return query;
    }
}

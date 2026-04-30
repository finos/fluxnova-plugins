package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricCaseActivityInstanceQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying historic case activity instances.
 */
@Schema(description = "Parameters for querying historic case activity instances.")
public record HistoricCaseActivityInstanceQueryDto(
        @Schema(description = "Filter by the id of the historic case activity instance.") String caseActivityInstanceId,
        @Schema(description = "Filter by a list of case activity instance ids.") List<String> caseActivityInstanceIdIn,
        @Schema(description = "Filter by the id of the case execution.") String caseExecutionId,
        @Schema(description = "Filter by the id of the case instance.") String caseInstanceId,
        @Schema(description = "Filter by the id of the case definition.") String caseDefinitionId,
        @Schema(description = "Filter by the id of the case activity.") String caseActivityId,
        @Schema(description = "Filter by a list of case activity ids.") List<String> caseActivityIdIn,
        @Schema(description = "Filter by the name of the case activity.") String caseActivityName,
        @Schema(description = "Filter by the type of the case activity.") String caseActivityType,
        @Schema(description = "Only return instances created before this date.") Date createdBefore,
        @Schema(description = "Only return instances created after this date.") Date createdAfter,
        @Schema(description = "Only return instances that ended before this date.") Date endedBefore,
        @Schema(description = "Only return instances that ended after this date.") Date endedAfter,
        @Schema(description = "If true, only required case activity instances are returned.") Boolean required,
        @Schema(description = "If true, only ended case activity instances are returned.") Boolean ended,
        @Schema(description = "If true, only case activity instances that have not ended are returned.") Boolean notEnded,
        @Schema(description = "If true, only available case activity instances are returned.") Boolean available,
        @Schema(description = "If true, only enabled case activity instances are returned.") Boolean enabled,
        @Schema(description = "If true, only disabled case activity instances are returned.") Boolean disabled,
        @Schema(description = "If true, only active case activity instances are returned.") Boolean active,
        @Schema(description = "If true, only completed case activity instances are returned.") Boolean completed,
        @Schema(description = "If true, only terminated case activity instances are returned.") Boolean terminated,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only instances without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricCaseActivityInstanceQuery toQuery(HistoricCaseActivityInstanceQuery query) {
        if (caseActivityInstanceId != null) query.caseActivityInstanceId(caseActivityInstanceId);
        if (caseActivityInstanceIdIn != null && !caseActivityInstanceIdIn.isEmpty())
            query.caseActivityInstanceIdIn(caseActivityInstanceIdIn.toArray(new String[0]));
        if (caseExecutionId != null) query.caseExecutionId(caseExecutionId);
        if (caseInstanceId != null) query.caseInstanceId(caseInstanceId);
        if (caseDefinitionId != null) query.caseDefinitionId(caseDefinitionId);
        if (caseActivityId != null) query.caseActivityId(caseActivityId);
        if (caseActivityIdIn != null && !caseActivityIdIn.isEmpty())
            query.caseActivityIdIn(caseActivityIdIn.toArray(new String[0]));
        if (caseActivityName != null) query.caseActivityName(caseActivityName);
        if (caseActivityType != null) query.caseActivityType(caseActivityType);
        if (createdBefore != null) query.createdBefore(createdBefore);
        if (createdAfter != null) query.createdAfter(createdAfter);
        if (endedBefore != null) query.endedBefore(endedBefore);
        if (endedAfter != null) query.endedAfter(endedAfter);
        if (Boolean.TRUE.equals(required)) query.required();
        if (Boolean.TRUE.equals(ended)) query.ended();
        if (Boolean.TRUE.equals(notEnded)) query.notEnded();
        if (Boolean.TRUE.equals(available)) query.available();
        if (Boolean.TRUE.equals(enabled)) query.enabled();
        if (Boolean.TRUE.equals(disabled)) query.disabled();
        if (Boolean.TRUE.equals(active)) query.active();
        if (Boolean.TRUE.equals(completed)) query.completed();
        if (Boolean.TRUE.equals(terminated)) query.terminated();
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        return query;
    }
}

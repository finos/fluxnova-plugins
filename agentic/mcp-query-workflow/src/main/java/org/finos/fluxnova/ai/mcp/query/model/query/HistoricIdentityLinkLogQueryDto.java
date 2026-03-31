package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricIdentityLinkLogQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying historic identity link log entries.
 */
@Schema(description = "Parameters for querying historic identity link log entries.")
public record HistoricIdentityLinkLogQueryDto(
        @Schema(description = "Only return entries with a time before this date.") Date dateBefore,
        @Schema(description = "Only return entries with a time after this date.") Date dateAfter,
        @Schema(description = "Filter by the type of the identity link (e.g. candidate, assignee).") String type,
        @Schema(description = "Filter by the id of the user.") String userId,
        @Schema(description = "Filter by the id of the group.") String groupId,
        @Schema(description = "Filter by the id of the task.") String taskId,
        @Schema(description = "Filter by the id of the process definition.") String processDefinitionId,
        @Schema(description = "Filter by the key of the process definition.") String processDefinitionKey,
        @Schema(description = "Filter by the type of the operation (add or delete).") String operationType,
        @Schema(description = "Filter by the id of the user who performed the operation.") String assignerId,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only entries without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricIdentityLinkLogQuery toQuery(HistoricIdentityLinkLogQuery query) {
        if (dateBefore != null) query.dateBefore(dateBefore);
        if (dateAfter != null) query.dateAfter(dateAfter);
        if (type != null) query.type(type);
        if (userId != null) query.userId(userId);
        if (groupId != null) query.groupId(groupId);
        if (taskId != null) query.taskId(taskId);
        if (processDefinitionId != null) query.processDefinitionId(processDefinitionId);
        if (processDefinitionKey != null) query.processDefinitionKey(processDefinitionKey);
        if (operationType != null) query.operationType(operationType);
        if (assignerId != null) query.assignerId(assignerId);
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        return query;
    }
}

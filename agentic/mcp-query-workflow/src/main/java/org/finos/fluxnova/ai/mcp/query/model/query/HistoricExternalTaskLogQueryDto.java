package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricExternalTaskLogQuery;

import java.util.List;

/**
 * DTO for querying historic external task logs.
 */
@Schema(description = "Parameters for querying historic external task logs.")
public record HistoricExternalTaskLogQueryDto(
        @Schema(description = "Filter by the id of the historic external task log entry.") String logId,
        @Schema(description = "Filter by the id of the external task.") String externalTaskId,
        @Schema(description = "Filter by the topic name of the external task.") String topicName,
        @Schema(description = "Filter by the id of the worker that locked the external task.") String workerId,
        @Schema(description = "Filter by the error message of the external task.") String errorMessage,
        @Schema(description = "Filter by a list of activity ids.") List<String> activityIdIn,
        @Schema(description = "Filter by a list of activity instance ids.") List<String> activityInstanceIdIn,
        @Schema(description = "Filter by a list of execution ids.") List<String> executionIdIn,
        @Schema(description = "Filter by the id of the process instance.") String processInstanceId,
        @Schema(description = "Filter by the id of the process definition.") String processDefinitionId,
        @Schema(description = "Filter by the key of the process definition.") String processDefinitionKey,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only log entries without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Only return log entries with a priority higher than or equal to this value.") Long priorityHigherThanOrEquals,
        @Schema(description = "Only return log entries with a priority lower than or equal to this value.") Long priorityLowerThanOrEquals,
        @Schema(description = "If true, only creation log entries are returned.") Boolean creationLog,
        @Schema(description = "If true, only failure log entries are returned.") Boolean failureLog,
        @Schema(description = "If true, only success log entries are returned.") Boolean successLog,
        @Schema(description = "If true, only deletion log entries are returned.") Boolean deletionLog,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricExternalTaskLogQuery toQuery(HistoricExternalTaskLogQuery query) {
        if (logId != null) query.logId(logId);
        if (externalTaskId != null) query.externalTaskId(externalTaskId);
        if (topicName != null) query.topicName(topicName);
        if (workerId != null) query.workerId(workerId);
        if (errorMessage != null) query.errorMessage(errorMessage);
        if (activityIdIn != null && !activityIdIn.isEmpty())
            query.activityIdIn(activityIdIn.toArray(new String[0]));
        if (activityInstanceIdIn != null && !activityInstanceIdIn.isEmpty())
            query.activityInstanceIdIn(activityInstanceIdIn.toArray(new String[0]));
        if (executionIdIn != null && !executionIdIn.isEmpty())
            query.executionIdIn(executionIdIn.toArray(new String[0]));
        if (processInstanceId != null) query.processInstanceId(processInstanceId);
        if (processDefinitionId != null) query.processDefinitionId(processDefinitionId);
        if (processDefinitionKey != null) query.processDefinitionKey(processDefinitionKey);
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        if (priorityHigherThanOrEquals != null)
            query.priorityHigherThanOrEquals(priorityHigherThanOrEquals);
        if (priorityLowerThanOrEquals != null)
            query.priorityLowerThanOrEquals(priorityLowerThanOrEquals);
        if (Boolean.TRUE.equals(creationLog)) query.creationLog();
        if (Boolean.TRUE.equals(failureLog)) query.failureLog();
        if (Boolean.TRUE.equals(successLog)) query.successLog();
        if (Boolean.TRUE.equals(deletionLog)) query.deletionLog();
        return query;
    }
}

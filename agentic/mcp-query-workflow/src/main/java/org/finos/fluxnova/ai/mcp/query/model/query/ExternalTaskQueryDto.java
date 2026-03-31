package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.ExternalTaskService;
import org.finos.fluxnova.bpm.engine.externaltask.ExternalTaskQuery;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * DTO for querying external tasks via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering external tasks.")
public record ExternalTaskQueryDto(
        @Schema(description = "Filter by the id of the external task.")
        String externalTaskId,

        @Schema(description = "Filter by a list of external task ids.")
        List<String> externalTaskIdIn,

        @Schema(description = "Filter by the id of the worker that most recently locked the external task.")
        String workerId,

        @Schema(description = "Filter by external tasks whose lock expires before the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date lockExpirationBefore,

        @Schema(description = "Filter by external tasks whose lock expires after the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date lockExpirationAfter,

        @Schema(description = "Filter by the topic name of the external task.")
        String topicName,

        @Schema(description = "Only include external tasks that are currently locked. "
                + "Value may only be true, as false is the default behavior.")
        Boolean locked,

        @Schema(description = "Only include external tasks that are not currently locked. "
                + "Value may only be true, as false is the default behavior.")
        Boolean notLocked,

        @Schema(description = "Filter by the id of the execution the external task belongs to.")
        String executionId,

        @Schema(description = "Filter by the id of the process instance the external task belongs to.")
        String processInstanceId,

        @Schema(description = "Filter by a list of process instance ids. An external task must belong to one of the given process instances.")
        List<String> processInstanceIdIn,

        @Schema(description = "Filter by the id of the process definition the external task belongs to.")
        String processDefinitionId,

        @Schema(description = "Filter by the id of the activity the external task belongs to.")
        String activityId,

        @Schema(description = "Filter by a list of activity ids. An external task must belong to one of the given activities.")
        List<String> activityIdIn,

        @Schema(description = "Only include external tasks with a priority greater than or equal to the given value.")
        Long priorityHigherThanOrEquals,

        @Schema(description = "Only include external tasks with a priority lower than or equal to the given value.")
        Long priorityLowerThanOrEquals,

        @Schema(description = "Only include external tasks that are currently suspended. "
                + "Value may only be true, as false is the default behavior.")
        Boolean suspended,

        @Schema(description = "Only include external tasks that are currently active (not suspended). "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include external tasks that have retries remaining (retries > 0). "
                + "Value may only be true, as false is the default behavior.")
        Boolean withRetriesLeft,

        @Schema(description = "Only include external tasks that have no retries remaining (retries = 0). "
                + "Value may only be true, as false is the default behavior.")
        Boolean noRetriesLeft,

        @Schema(description = "Filter by a list of tenant ids. An external task must belong to one of the given tenants.")
        List<String> tenantIdIn
) {
    public ExternalTaskQuery toQuery(ExternalTaskService externalTaskService) {
        ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();
        if (externalTaskId != null) {
            query.externalTaskId(externalTaskId);
        }
        if (externalTaskIdIn != null && !externalTaskIdIn.isEmpty()) {
            query.externalTaskIdIn(new HashSet<>(externalTaskIdIn));
        }
        if (workerId != null) {
            query.workerId(workerId);
        }
        if (lockExpirationBefore != null) {
            query.lockExpirationBefore(lockExpirationBefore);
        }
        if (lockExpirationAfter != null) {
            query.lockExpirationAfter(lockExpirationAfter);
        }
        if (topicName != null) {
            query.topicName(topicName);
        }
        if (Boolean.TRUE.equals(locked)) {
            query.locked();
        }
        if (Boolean.TRUE.equals(notLocked)) {
            query.notLocked();
        }
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (processInstanceIdIn != null && !processInstanceIdIn.isEmpty()) {
            query.processInstanceIdIn(processInstanceIdIn.toArray(new String[0]));
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (activityId != null) {
            query.activityId(activityId);
        }
        if (activityIdIn != null && !activityIdIn.isEmpty()) {
            query.activityIdIn(activityIdIn.toArray(new String[0]));
        }
        if (priorityHigherThanOrEquals != null) {
            query.priorityHigherThanOrEquals(priorityHigherThanOrEquals);
        }
        if (priorityLowerThanOrEquals != null) {
            query.priorityLowerThanOrEquals(priorityLowerThanOrEquals);
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(withRetriesLeft)) {
            query.withRetriesLeft();
        }
        if (Boolean.TRUE.equals(noRetriesLeft)) {
            query.noRetriesLeft();
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        return query;
    }
}

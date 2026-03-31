package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.HistoryService;
import org.finos.fluxnova.bpm.engine.history.HistoricActivityInstanceQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying historic activity instances via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering historic activity instances.")
public record HistoricActivityInstanceQueryDto(
        @Schema(description = "Filter by the id of the activity instance.")
        String activityInstanceId,

        @Schema(description = "Filter by the id of the process instance the activity instance belongs to.")
        String processInstanceId,

        @Schema(description = "Filter by the id of the process definition.")
        String processDefinitionId,

        @Schema(description = "Filter by the id of the execution.")
        String executionId,

        @Schema(description = "Filter by the id of the activity (element in the process definition).")
        String activityId,

        @Schema(description = "Filter by the name of the activity.")
        String activityName,

        @Schema(description = "Filter by a name pattern (use % as wildcard).")
        String activityNameLike,

        @Schema(description = "Filter by the type of the activity (e.g. userTask, serviceTask, startEvent).")
        String activityType,

        @Schema(description = "Filter by the id of the task assignee.")
        String taskAssignee,

        @Schema(description = "Only include finished activity instances.")
        Boolean finished,

        @Schema(description = "Only include unfinished (currently running) activity instances.")
        Boolean unfinished,

        @Schema(description = "Only include activity instances that completed their scope.")
        Boolean completeScope,

        @Schema(description = "Only include canceled activity instances.")
        Boolean canceled,

        @Schema(description = "Filter by instances started before this date.")
        Date startedBefore,

        @Schema(description = "Filter by instances started after this date.")
        Date startedAfter,

        @Schema(description = "Filter by instances finished before this date.")
        Date finishedBefore,

        @Schema(description = "Filter by instances finished after this date.")
        Date finishedAfter,

        @Schema(description = "Filter by a list of tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include activity instances which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Maximum number of results to return.")
        Integer maxResults
) {
    public HistoricActivityInstanceQuery toQuery(HistoryService historyService) {
        HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();
        if (activityInstanceId != null) {
            query.activityInstanceId(activityInstanceId);
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (activityId != null) {
            query.activityId(activityId);
        }
        if (activityName != null) {
            query.activityName(activityName);
        }
        if (activityNameLike != null) {
            query.activityNameLike(activityNameLike);
        }
        if (activityType != null) {
            query.activityType(activityType);
        }
        if (taskAssignee != null) {
            query.taskAssignee(taskAssignee);
        }
        if (Boolean.TRUE.equals(finished)) {
            query.finished();
        }
        if (Boolean.TRUE.equals(unfinished)) {
            query.unfinished();
        }
        if (Boolean.TRUE.equals(completeScope)) {
            query.completeScope();
        }
        if (Boolean.TRUE.equals(canceled)) {
            query.canceled();
        }
        if (startedBefore != null) {
            query.startedBefore(startedBefore);
        }
        if (startedAfter != null) {
            query.startedAfter(startedAfter);
        }
        if (finishedBefore != null) {
            query.finishedBefore(finishedBefore);
        }
        if (finishedAfter != null) {
            query.finishedAfter(finishedAfter);
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

package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.HistoryService;
import org.finos.fluxnova.bpm.engine.history.HistoricTaskInstanceQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying historic task instances via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering historic task instances.")
public record HistoricTaskInstanceQueryDto(
        @Schema(description = "Filter by the id of the historic task instance.")
        String taskId,

        @Schema(description = "Filter by the id of the process instance the task belongs to.")
        String processInstanceId,

        @Schema(description = "Filter by the id of the root process instance.")
        String rootProcessInstanceId,

        @Schema(description = "Filter by the business key of the process instance.")
        String processInstanceBusinessKey,

        @Schema(description = "Filter by a list of business keys.")
        List<String> processInstanceBusinessKeyIn,

        @Schema(description = "Filter by a business key pattern (use % as wildcard).")
        String processInstanceBusinessKeyLike,

        @Schema(description = "Filter by the id of the execution.")
        String executionId,

        @Schema(description = "Filter by a list of activity instance ids.")
        List<String> activityInstanceIdIn,

        @Schema(description = "Filter by the id of the process definition.")
        String processDefinitionId,

        @Schema(description = "Filter by the key of the process definition.")
        String processDefinitionKey,

        @Schema(description = "Filter by the name of the process definition.")
        String processDefinitionName,

        @Schema(description = "Filter by the id of the case definition.")
        String caseDefinitionId,

        @Schema(description = "Filter by the key of the case definition.")
        String caseDefinitionKey,

        @Schema(description = "Filter by the name of the case definition.")
        String caseDefinitionName,

        @Schema(description = "Filter by the id of the case instance.")
        String caseInstanceId,

        @Schema(description = "Filter by the id of the case execution.")
        String caseExecutionId,

        @Schema(description = "Filter by the task name.")
        String taskName,

        @Schema(description = "Filter by a task name pattern (use % as wildcard).")
        String taskNameLike,

        @Schema(description = "Filter by the task description.")
        String taskDescription,

        @Schema(description = "Filter by a description pattern (use % as wildcard).")
        String taskDescriptionLike,

        @Schema(description = "Filter by the task definition key.")
        String taskDefinitionKey,

        @Schema(description = "Filter by a list of task definition keys.")
        List<String> taskDefinitionKeyIn,

        @Schema(description = "Filter by the delete reason.")
        String taskDeleteReason,

        @Schema(description = "Filter by a delete reason pattern (use % as wildcard).")
        String taskDeleteReasonLike,

        @Schema(description = "Only include tasks that are assigned.")
        Boolean taskAssigned,

        @Schema(description = "Only include tasks that are unassigned.")
        Boolean taskUnassigned,

        @Schema(description = "Filter by the task assignee.")
        String taskAssignee,

        @Schema(description = "Filter by an assignee pattern (use % as wildcard).")
        String taskAssigneeLike,

        @Schema(description = "Filter by the task owner.")
        String taskOwner,

        @Schema(description = "Filter by an owner pattern (use % as wildcard).")
        String taskOwnerLike,

        @Schema(description = "Filter by task priority.")
        Integer taskPriority,

        @Schema(description = "Only include tasks whose process has finished.")
        Boolean processFinished,

        @Schema(description = "Only include tasks whose process has not finished.")
        Boolean processUnfinished,

        @Schema(description = "Filter by a user that was involved in the task.")
        String taskInvolvedUser,

        @Schema(description = "Filter by a group that was involved in the task.")
        String taskInvolvedGroup,

        @Schema(description = "Filter by a candidate user the task once had.")
        String taskHadCandidateUser,

        @Schema(description = "Filter by a candidate group the task once had.")
        String taskHadCandidateGroup,

        @Schema(description = "Only include tasks that had at least one candidate group.")
        Boolean withCandidateGroups,

        @Schema(description = "Only include tasks that had no candidate groups.")
        Boolean withoutCandidateGroups,

        @Schema(description = "Filter by a parent task id.")
        String taskParentTaskId,

        @Schema(description = "Filter by tasks due before this date.")
        Date taskDueBefore,

        @Schema(description = "Filter by tasks due after this date.")
        Date taskDueAfter,

        @Schema(description = "Only include tasks without a due date.")
        Boolean withoutTaskDueDate,

        @Schema(description = "Filter by follow-up date before this date.")
        Date taskFollowUpBefore,

        @Schema(description = "Filter by follow-up date after this date.")
        Date taskFollowUpAfter,

        @Schema(description = "Filter by a list of tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include tasks which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Maximum number of results to return.")
        Integer maxResults
) {
    public HistoricTaskInstanceQuery toQuery(HistoryService historyService) {
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();
        if (taskId != null) {
            query.taskId(taskId);
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (rootProcessInstanceId != null) {
            query.rootProcessInstanceId(rootProcessInstanceId);
        }
        if (processInstanceBusinessKey != null) {
            query.processInstanceBusinessKey(processInstanceBusinessKey);
        }
        if (processInstanceBusinessKeyIn != null && !processInstanceBusinessKeyIn.isEmpty()) {
            query.processInstanceBusinessKeyIn(processInstanceBusinessKeyIn.toArray(new String[0]));
        }
        if (processInstanceBusinessKeyLike != null) {
            query.processInstanceBusinessKeyLike(processInstanceBusinessKeyLike);
        }
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (activityInstanceIdIn != null && !activityInstanceIdIn.isEmpty()) {
            query.activityInstanceIdIn(activityInstanceIdIn.toArray(new String[0]));
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (processDefinitionKey != null) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (processDefinitionName != null) {
            query.processDefinitionName(processDefinitionName);
        }
        if (caseDefinitionId != null) {
            query.caseDefinitionId(caseDefinitionId);
        }
        if (caseDefinitionKey != null) {
            query.caseDefinitionKey(caseDefinitionKey);
        }
        if (caseDefinitionName != null) {
            query.caseDefinitionName(caseDefinitionName);
        }
        if (caseInstanceId != null) {
            query.caseInstanceId(caseInstanceId);
        }
        if (caseExecutionId != null) {
            query.caseExecutionId(caseExecutionId);
        }
        if (taskName != null) {
            query.taskName(taskName);
        }
        if (taskNameLike != null) {
            query.taskNameLike(taskNameLike);
        }
        if (taskDescription != null) {
            query.taskDescription(taskDescription);
        }
        if (taskDescriptionLike != null) {
            query.taskDescriptionLike(taskDescriptionLike);
        }
        if (taskDefinitionKey != null) {
            query.taskDefinitionKey(taskDefinitionKey);
        }
        if (taskDefinitionKeyIn != null && !taskDefinitionKeyIn.isEmpty()) {
            query.taskDefinitionKeyIn(taskDefinitionKeyIn.toArray(new String[0]));
        }
        if (taskDeleteReason != null) {
            query.taskDeleteReason(taskDeleteReason);
        }
        if (taskDeleteReasonLike != null) {
            query.taskDeleteReasonLike(taskDeleteReasonLike);
        }
        if (Boolean.TRUE.equals(taskAssigned)) {
            query.taskAssigned();
        }
        if (Boolean.TRUE.equals(taskUnassigned)) {
            query.taskUnassigned();
        }
        if (taskAssignee != null) {
            query.taskAssignee(taskAssignee);
        }
        if (taskAssigneeLike != null) {
            query.taskAssigneeLike(taskAssigneeLike);
        }
        if (taskOwner != null) {
            query.taskOwner(taskOwner);
        }
        if (taskOwnerLike != null) {
            query.taskOwnerLike(taskOwnerLike);
        }
        if (taskPriority != null) {
            query.taskPriority(taskPriority);
        }
        if (Boolean.TRUE.equals(processFinished)) {
            query.processFinished();
        }
        if (Boolean.TRUE.equals(processUnfinished)) {
            query.processUnfinished();
        }
        if (taskInvolvedUser != null) {
            query.taskInvolvedUser(taskInvolvedUser);
        }
        if (taskInvolvedGroup != null) {
            query.taskInvolvedGroup(taskInvolvedGroup);
        }
        if (taskHadCandidateUser != null) {
            query.taskHadCandidateUser(taskHadCandidateUser);
        }
        if (taskHadCandidateGroup != null) {
            query.taskHadCandidateGroup(taskHadCandidateGroup);
        }
        if (Boolean.TRUE.equals(withCandidateGroups)) {
            query.withCandidateGroups();
        }
        if (Boolean.TRUE.equals(withoutCandidateGroups)) {
            query.withoutCandidateGroups();
        }
        if (taskParentTaskId != null) {
            query.taskParentTaskId(taskParentTaskId);
        }
        if (taskDueBefore != null) {
            query.taskDueBefore(taskDueBefore);
        }
        if (taskDueAfter != null) {
            query.taskDueAfter(taskDueAfter);
        }
        if (Boolean.TRUE.equals(withoutTaskDueDate)) {
            query.withoutTaskDueDate();
        }
        if (taskFollowUpBefore != null) {
            query.taskFollowUpBefore(taskFollowUpBefore);
        }
        if (taskFollowUpAfter != null) {
            query.taskFollowUpAfter(taskFollowUpAfter);
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

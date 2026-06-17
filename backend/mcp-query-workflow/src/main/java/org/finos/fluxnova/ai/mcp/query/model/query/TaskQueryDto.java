package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.TaskService;
import org.finos.fluxnova.bpm.engine.task.DelegationState;
import org.finos.fluxnova.bpm.engine.task.TaskQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying tasks via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering tasks.")
public record TaskQueryDto(
        @Schema(description = "Restrict to task with the given id.")
        String taskId,

        @Schema(description = "Restrict to tasks with any of the given ids.")
        List<String> taskIdIn,

        @Schema(description = "Restrict to tasks that belong to process instances with the given id.")
        String processInstanceId,

        @Schema(description = "Restrict to tasks that belong to process instances with the given ids.")
        List<String> processInstanceIdIn,

        @Schema(description = "Restrict to tasks that belong to process instances with the given business key.")
        String processInstanceBusinessKey,

        @Schema(description = "Restrict to tasks that belong to process instances with one of the given business keys. "
                + "The keys need to be in a comma-separated list.")
        List<String> processInstanceBusinessKeyIn,

        @Schema(description = "Restrict to tasks that have a process instance business key that has the parameter value "
                + "as a substring.")
        String processInstanceBusinessKeyLike,

        @Schema(description = "Restrict to tasks that belong to a process definition with the given id.")
        String processDefinitionId,

        @Schema(description = "Restrict to tasks that belong to a process definition with the given key.")
        String processDefinitionKey,

        @Schema(description = "Restrict to tasks that belong to a process definition with one of the given keys.")
        List<String> processDefinitionKeyIn,

        @Schema(description = "Restrict to tasks that belong to a process definition with the given name.")
        String processDefinitionName,

        @Schema(description = "Restrict to tasks that have a process definition name that has the parameter value "
                + "as a substring.")
        String processDefinitionNameLike,

        @Schema(description = "Restrict to tasks that belong to an execution with the given id.")
        String executionId,

        @Schema(description = "Restrict to tasks that the given user is assigned to.")
        String assignee,

        @Schema(description = "Restrict to tasks that the user described by the given expression is assigned to.")
        String assigneeLike,

        @Schema(description = "Restrict to tasks that are assigned to users with one of the given ids.")
        List<String> assigneeIn,

        @Schema(description = "Restrict to tasks that the given user owns.")
        String owner,

        @Schema(description = "Only include tasks that are offered to the given group.")
        String candidateGroup,

        @Schema(description = "Only include tasks that are offered to the given user or to one of his groups.")
        String candidateUser,

        @Schema(description = "Only include tasks that are offered to one of the given candidate groups.")
        List<String> candidateGroups,

        @Schema(description = "Restrict to tasks that the given user or any of the user's candidate groups is involved in. "
                + "A user is involved in a task if the user is the assignee, the owner, one of the candidate users, "
                + "or a member of one of the candidate groups.")
        String involvedUser,

        @Schema(description = "If set to true, restricts the query to all tasks that are assigned.")
        Boolean assigned,

        @Schema(description = "If set to true, restricts the query to all tasks that are unassigned.")
        Boolean unassigned,

        @Schema(description = "Restrict to tasks that have the given key.")
        String taskDefinitionKey,

        @Schema(description = "Restrict to tasks that have one of the given keys.")
        List<String> taskDefinitionKeyIn,

        @Schema(description = "Restrict to tasks that have a key that has the parameter value as a substring.")
        String taskDefinitionKeyLike,

        @Schema(description = "Restrict to tasks that have the given name.")
        String name,

        @Schema(description = "Restrict to tasks that do not have the given name.")
        String nameNotEqual,

        @Schema(description = "Restrict to tasks that have a name with the given parameter value as a substring.")
        String nameLike,

        @Schema(description = "Restrict to tasks that do not have a name with the given parameter value as a substring.")
        String nameNotLike,

        @Schema(description = "Restrict to tasks that have the given description.")
        String description,

        @Schema(description = "Restrict to tasks that have a description that has the parameter value as a substring.")
        String descriptionLike,

        @Schema(description = "Restrict to tasks that have the given priority.")
        Integer priority,

        @Schema(description = "Restrict to tasks that have a lower or equal priority.")
        Integer maxPriority,

        @Schema(description = "Restrict to tasks that have a higher or equal priority.")
        Integer minPriority,

        @Schema(description = "Restrict to tasks that are due on the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date dueDate,

        @Schema(description = "Restrict to tasks that are due after the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date dueAfter,

        @Schema(description = "Restrict to tasks that are due before the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date dueBefore,

        @Schema(description = "Only include tasks which have no due date. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withoutDueDate,

        @Schema(description = "Restrict to tasks that have a followUp date on the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date followUpDate,

        @Schema(description = "Restrict to tasks that have a followUp date after the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date followUpAfter,

        @Schema(description = "Restrict to tasks that have a followUp date before the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date followUpBefore,

        @Schema(description = "Restrict to tasks that have no followUp date or a followUp date before the given date. "
                + "Serves the typical use case 'give me all tasks without follow-up or follow-up date which is already due'.")
        Date followUpBeforeOrNotExistent,

        @Schema(description = "Restrict to tasks that were created on the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date createdOn,

        @Schema(description = "Restrict to tasks that were created after the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date createdAfter,

        @Schema(description = "Restrict to tasks that were created before the given date. "
                + "By default, the date must not include a time component - only dates "
                + "(e.g., 2013-01-23) are supported. To include a time and timezone, the "
                + "format must be yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2013-01-23T14:42:45.000+0200).")
        Date createdBefore,

        @Schema(description = "Restrict to tasks that were updated after the given date. "
                + "Every action on a task (e.g. claim, delegate, complete) updates the timestamp.")
        Date updatedAfter,

        @Schema(description = "Restrict to tasks that have the given delegation state.",
                allowableValues = {"PENDING", "RESOLVED"})
        String delegationState,

        @Schema(description = "Only include tasks which have candidate groups. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withCandidateGroups,

        @Schema(description = "Only include tasks which have no candidate groups. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withoutCandidateGroups,

        @Schema(description = "Only include tasks which have candidate users. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withCandidateUsers,

        @Schema(description = "Only include tasks which have no candidate users. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withoutCandidateUsers,

        @Schema(description = "Only include active tasks. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include suspended tasks. "
                + "Value may only be true, as false is the default behavior.")
        Boolean suspended,

        @Schema(description = "Filter by a list of tenant ids. A task must have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include tasks which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Restrict to tasks that are sub tasks of the given task. "
                + "Takes a task id.")
        String parentTaskId,

        @Schema(description = "Restrict to tasks that belong to a case instance with the given id.")
        String caseInstanceId,

        @Schema(description = "Restrict to tasks that belong to a case instance with the given business key.")
        String caseInstanceBusinessKey,

        @Schema(description = "Restrict to tasks that have a case instance business key that has the parameter value "
                + "as a substring.")
        String caseInstanceBusinessKeyLike,

        @Schema(description = "Restrict to tasks that belong to a case definition with the given id.")
        String caseDefinitionId,

        @Schema(description = "Restrict to tasks that belong to a case definition with the given key.")
        String caseDefinitionKey,

        @Schema(description = "Restrict to tasks that belong to a case definition with the given name.")
        String caseDefinitionName,

        @Schema(description = "Restrict to tasks that have a case definition name that has the parameter value as a substring.")
        String caseDefinitionNameLike,

        @Schema(description = "Restrict to tasks that belong to a case execution with the given id.")
        String caseExecutionId,

        @Schema(description = "Only select tasks which have no parent (i.e. do not select subtasks). "
                + "Value may only be true, as false is the default behavior.")
        Boolean excludeSubtasks
) {
    /**
     * Create a new TaskQuery from the TaskService, with all non-null filter criteria applied.
     */
    public TaskQuery toQuery(TaskService taskService) {
        TaskQuery query = taskService.createTaskQuery();
        if (taskId != null) {
            query.taskId(taskId);
        }
        if (taskIdIn != null && !taskIdIn.isEmpty()) {
            query.taskIdIn(taskIdIn.toArray(new String[0]));
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (processInstanceIdIn != null && !processInstanceIdIn.isEmpty()) {
            query.processInstanceIdIn(processInstanceIdIn.toArray(new String[0]));
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
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (processDefinitionKey != null) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (processDefinitionKeyIn != null && !processDefinitionKeyIn.isEmpty()) {
            query.processDefinitionKeyIn(processDefinitionKeyIn.toArray(new String[0]));
        }
        if (processDefinitionName != null) {
            query.processDefinitionName(processDefinitionName);
        }
        if (processDefinitionNameLike != null) {
            query.processDefinitionNameLike(processDefinitionNameLike);
        }
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (assignee != null) {
            query.taskAssignee(assignee);
        }
        if (assigneeLike != null) {
            query.taskAssigneeLike(assigneeLike);
        }
        if (assigneeIn != null && !assigneeIn.isEmpty()) {
            query.taskAssigneeIn(assigneeIn.toArray(new String[0]));
        }
        if (owner != null) {
            query.taskOwner(owner);
        }
        if (candidateGroup != null) {
            query.taskCandidateGroup(candidateGroup);
        }
        if (candidateUser != null) {
            query.taskCandidateUser(candidateUser);
        }
        if (candidateGroups != null && !candidateGroups.isEmpty()) {
            query.taskCandidateGroupIn(candidateGroups);
        }
        if (involvedUser != null) {
            query.taskInvolvedUser(involvedUser);
        }
        if (Boolean.TRUE.equals(assigned)) {
            query.taskAssigned();
        }
        if (Boolean.TRUE.equals(unassigned)) {
            query.taskUnassigned();
        }
        if (taskDefinitionKey != null) {
            query.taskDefinitionKey(taskDefinitionKey);
        }
        if (taskDefinitionKeyIn != null && !taskDefinitionKeyIn.isEmpty()) {
            query.taskDefinitionKeyIn(taskDefinitionKeyIn.toArray(new String[0]));
        }
        if (taskDefinitionKeyLike != null) {
            query.taskDefinitionKeyLike(taskDefinitionKeyLike);
        }
        if (name != null) {
            query.taskName(name);
        }
        if (nameNotEqual != null) {
            query.taskNameNotEqual(nameNotEqual);
        }
        if (nameLike != null) {
            query.taskNameLike(nameLike);
        }
        if (nameNotLike != null) {
            query.taskNameNotLike(nameNotLike);
        }
        if (description != null) {
            query.taskDescription(description);
        }
        if (descriptionLike != null) {
            query.taskDescriptionLike(descriptionLike);
        }
        if (priority != null) {
            query.taskPriority(priority);
        }
        if (maxPriority != null) {
            query.taskMaxPriority(maxPriority);
        }
        if (minPriority != null) {
            query.taskMinPriority(minPriority);
        }
        if (dueDate != null) {
            query.dueDate(dueDate);
        }
        if (dueAfter != null) {
            query.dueAfter(dueAfter);
        }
        if (dueBefore != null) {
            query.dueBefore(dueBefore);
        }
        if (Boolean.TRUE.equals(withoutDueDate)) {
            query.withoutDueDate();
        }
        if (followUpDate != null) {
            query.followUpDate(followUpDate);
        }
        if (followUpAfter != null) {
            query.followUpAfter(followUpAfter);
        }
        if (followUpBefore != null) {
            query.followUpBefore(followUpBefore);
        }
        if (followUpBeforeOrNotExistent != null) {
            query.followUpBeforeOrNotExistent(followUpBeforeOrNotExistent);
        }
        if (createdOn != null) {
            query.taskCreatedOn(createdOn);
        }
        if (createdAfter != null) {
            query.taskCreatedAfter(createdAfter);
        }
        if (createdBefore != null) {
            query.taskCreatedBefore(createdBefore);
        }
        if (updatedAfter != null) {
            query.taskUpdatedAfter(updatedAfter);
        }
        if (delegationState != null) {
            query.taskDelegationState(DelegationState.valueOf(delegationState));
        }
        if (Boolean.TRUE.equals(withCandidateGroups)) {
            query.withCandidateGroups();
        }
        if (Boolean.TRUE.equals(withoutCandidateGroups)) {
            query.withoutCandidateGroups();
        }
        if (Boolean.TRUE.equals(withCandidateUsers)) {
            query.withCandidateUsers();
        }
        if (Boolean.TRUE.equals(withoutCandidateUsers)) {
            query.withoutCandidateUsers();
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (parentTaskId != null) {
            query.taskParentTaskId(parentTaskId);
        }
        if (caseInstanceId != null) {
            query.caseInstanceId(caseInstanceId);
        }
        if (caseInstanceBusinessKey != null) {
            query.caseInstanceBusinessKey(caseInstanceBusinessKey);
        }
        if (caseInstanceBusinessKeyLike != null) {
            query.caseInstanceBusinessKeyLike(caseInstanceBusinessKeyLike);
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
        if (caseDefinitionNameLike != null) {
            query.caseDefinitionNameLike(caseDefinitionNameLike);
        }
        if (caseExecutionId != null) {
            query.caseExecutionId(caseExecutionId);
        }
        if (Boolean.TRUE.equals(excludeSubtasks)) {
            query.excludeSubtasks();
        }
        query.initializeFormKeys();
        return query;
    }
}

package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.task.DelegationState;
import org.finos.fluxnova.bpm.engine.task.Task;

import java.util.Date;

/**
 * Result DTO for task query results.
 * Maps the fields from the engine's {@link Task} interface.
 */
public record TaskResultDto(
        String id,
        String name,
        String assignee,
        String owner,
        Date created,
        Date lastUpdated,
        Date due,
        Date followUp,
        String delegationState,
        String description,
        String executionId,
        String parentTaskId,
        Integer priority,
        String processDefinitionId,
        String processInstanceId,
        String caseExecutionId,
        String caseDefinitionId,
        String caseInstanceId,
        String taskDefinitionKey,
        boolean suspended,
        String formKey,
        String tenantId,
        String taskState
) {
    public static TaskResultDto fromTask(Task task) {
        DelegationState ds = task.getDelegationState();
        return new TaskResultDto(
                task.getId(),
                task.getName(),
                task.getAssignee(),
                task.getOwner(),
                task.getCreateTime(),
                task.getLastUpdated(),
                task.getDueDate(),
                task.getFollowUpDate(),
                ds != null ? ds.name() : null,
                task.getDescription(),
                task.getExecutionId(),
                task.getParentTaskId(),
                task.getPriority(),
                task.getProcessDefinitionId(),
                task.getProcessInstanceId(),
                task.getCaseExecutionId(),
                task.getCaseDefinitionId(),
                task.getCaseInstanceId(),
                task.getTaskDefinitionKey(),
                task.isSuspended(),
                task.getFormKey(),
                task.getTenantId(),
                task.getTaskState()
        );
    }
}

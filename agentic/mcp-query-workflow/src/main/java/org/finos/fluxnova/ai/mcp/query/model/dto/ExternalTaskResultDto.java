package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.externaltask.ExternalTask;

import java.util.Date;

/**
 * Result DTO for external task query results.
 * Maps the fields from the engine's {@link ExternalTask} interface.
 */
public record ExternalTaskResultDto(
        String id,
        String topicName,
        String workerId,
        Date lockExpirationTime,
        Date createTime,
        String processInstanceId,
        String executionId,
        String activityId,
        String activityInstanceId,
        String processDefinitionId,
        String processDefinitionKey,
        String processDefinitionVersionTag,
        Integer retries,
        String errorMessage,
        boolean suspended,
        String tenantId,
        long priority,
        String businessKey
) {
    public static ExternalTaskResultDto fromExternalTask(ExternalTask externalTask) {
        return new ExternalTaskResultDto(
                externalTask.getId(),
                externalTask.getTopicName(),
                externalTask.getWorkerId(),
                externalTask.getLockExpirationTime(),
                externalTask.getCreateTime(),
                externalTask.getProcessInstanceId(),
                externalTask.getExecutionId(),
                externalTask.getActivityId(),
                externalTask.getActivityInstanceId(),
                externalTask.getProcessDefinitionId(),
                externalTask.getProcessDefinitionKey(),
                externalTask.getProcessDefinitionVersionTag(),
                externalTask.getRetries(),
                externalTask.getErrorMessage(),
                externalTask.isSuspended(),
                externalTask.getTenantId(),
                externalTask.getPriority(),
                externalTask.getBusinessKey()
        );
    }
}

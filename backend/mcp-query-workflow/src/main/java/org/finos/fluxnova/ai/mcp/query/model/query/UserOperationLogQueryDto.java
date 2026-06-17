package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.UserOperationLogQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying user operation log entries.
 */
@Schema(description = "Parameters for querying user operation log entries.")
public record UserOperationLogQueryDto(
        @Schema(description = "Filter by the type of entity affected (e.g. Task, ProcessInstance).") String entityType,
        @Schema(description = "Filter by a list of entity types.") List<String> entityTypeIn,
        @Schema(description = "Filter by the type of the operation.") String operationType,
        @Schema(description = "Filter by the id of the deployment.") String deploymentId,
        @Schema(description = "Filter by the id of the process definition.") String processDefinitionId,
        @Schema(description = "Filter by the key of the process definition.") String processDefinitionKey,
        @Schema(description = "Filter by the id of the process instance.") String processInstanceId,
        @Schema(description = "Filter by the id of the execution.") String executionId,
        @Schema(description = "Filter by the id of the case definition.") String caseDefinitionId,
        @Schema(description = "Filter by the id of the case instance.") String caseInstanceId,
        @Schema(description = "Filter by the id of the case execution.") String caseExecutionId,
        @Schema(description = "Filter by the id of the task.") String taskId,
        @Schema(description = "Filter by the id of the job.") String jobId,
        @Schema(description = "Filter by the id of the job definition.") String jobDefinitionId,
        @Schema(description = "Filter by the id of the batch.") String batchId,
        @Schema(description = "Filter by the id of the user who performed the operation.") String userId,
        @Schema(description = "Filter by the id of the operation.") String operationId,
        @Schema(description = "Filter by the id of the external task.") String externalTaskId,
        @Schema(description = "Filter by the property that was changed.") String property,
        @Schema(description = "Filter by the category of the operation.") String category,
        @Schema(description = "Filter by a list of categories.") List<String> categoryIn,
        @Schema(description = "Only return entries that occurred after this timestamp.") Date afterTimestamp,
        @Schema(description = "Only return entries that occurred before this timestamp.") Date beforeTimestamp,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only entries without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public UserOperationLogQuery toQuery(UserOperationLogQuery query) {
        if (entityType != null) query.entityType(entityType);
        if (entityTypeIn != null && !entityTypeIn.isEmpty())
            query.entityTypeIn(entityTypeIn.toArray(new String[0]));
        if (operationType != null) query.operationType(operationType);
        if (deploymentId != null) query.deploymentId(deploymentId);
        if (processDefinitionId != null) query.processDefinitionId(processDefinitionId);
        if (processDefinitionKey != null) query.processDefinitionKey(processDefinitionKey);
        if (processInstanceId != null) query.processInstanceId(processInstanceId);
        if (executionId != null) query.executionId(executionId);
        if (caseDefinitionId != null) query.caseDefinitionId(caseDefinitionId);
        if (caseInstanceId != null) query.caseInstanceId(caseInstanceId);
        if (caseExecutionId != null) query.caseExecutionId(caseExecutionId);
        if (taskId != null) query.taskId(taskId);
        if (jobId != null) query.jobId(jobId);
        if (jobDefinitionId != null) query.jobDefinitionId(jobDefinitionId);
        if (batchId != null) query.batchId(batchId);
        if (userId != null) query.userId(userId);
        if (operationId != null) query.operationId(operationId);
        if (externalTaskId != null) query.externalTaskId(externalTaskId);
        if (property != null) query.property(property);
        if (category != null) query.category(category);
        if (categoryIn != null && !categoryIn.isEmpty())
            query.categoryIn(categoryIn.toArray(new String[0]));
        if (afterTimestamp != null) query.afterTimestamp(afterTimestamp);
        if (beforeTimestamp != null) query.beforeTimestamp(beforeTimestamp);
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        return query;
    }
}

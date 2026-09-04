package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.runtime.VariableInstance;

/**
 * Result DTO for variable instance query results.
 * Maps the fields from the engine's {@link VariableInstance} interface.
 * <p>
 * Note: The variable value is serialized as an Object. Complex or binary values
 * may not serialize cleanly to JSON and will be represented as their toString() output.
 */
public record VariableInstanceResultDto(
        String id,
        String name,
        Object value,
        String typeName,
        String processInstanceId,
        String processDefinitionId,
        String executionId,
        String caseInstanceId,
        String caseExecutionId,
        String taskId,
        String batchId,
        String activityInstanceId,
        String tenantId,
        String errorMessage
) {
    public static VariableInstanceResultDto fromVariableInstance(VariableInstance variableInstance) {
        Object resolvedValue;
        String resolvedErrorMessage = variableInstance.getErrorMessage();
        try {
            resolvedValue = variableInstance.getValue();
        } catch (Exception e) {
            resolvedValue = null;
            resolvedErrorMessage = "Could not retrieve variable value: " + e.getMessage();
        }
        return new VariableInstanceResultDto(
                variableInstance.getId(),
                variableInstance.getName(),
                resolvedValue,
                variableInstance.getTypeName(),
                variableInstance.getProcessInstanceId(),
                variableInstance.getProcessDefinitionId(),
                variableInstance.getExecutionId(),
                variableInstance.getCaseInstanceId(),
                variableInstance.getCaseExecutionId(),
                variableInstance.getTaskId(),
                variableInstance.getBatchId(),
                variableInstance.getActivityInstanceId(),
                variableInstance.getTenantId(),
                resolvedErrorMessage
        );
    }
}

package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.runtime.Execution;

/**
 * Result DTO for execution query results.
 * Maps the fields from the engine's {@link Execution} interface.
 */
public record ExecutionResultDto(
        String id,
        String processInstanceId,
        boolean suspended,
        boolean ended,
        String tenantId
) {
    public static ExecutionResultDto fromExecution(Execution execution) {
        return new ExecutionResultDto(
                execution.getId(),
                execution.getProcessInstanceId(),
                execution.isSuspended(),
                execution.isEnded(),
                execution.getTenantId()
        );
    }
}

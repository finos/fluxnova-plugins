package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.runtime.ProcessInstance;

/**
 * Result DTO for process instance query results.
 * Maps the fields from the engine's {@link ProcessInstance} interface.
 */
public record ProcessInstanceResultDto(
        String id,
        String processDefinitionId,
        String businessKey,
        String rootProcessInstanceId,
        String caseInstanceId,
        boolean suspended,
        String tenantId
) {
    public static ProcessInstanceResultDto fromProcessInstance(ProcessInstance processInstance) {
        return new ProcessInstanceResultDto(
                processInstance.getId(),
                processInstance.getProcessDefinitionId(),
                processInstance.getBusinessKey(),
                processInstance.getRootProcessInstanceId(),
                processInstance.getCaseInstanceId(),
                processInstance.isSuspended(),
                processInstance.getTenantId()
        );
    }
}

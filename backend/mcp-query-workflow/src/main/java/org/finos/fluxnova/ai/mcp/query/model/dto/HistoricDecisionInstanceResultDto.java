package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.history.HistoricDecisionInstance;

import java.util.Date;

/**
 * DTO representing a single historic decision instance result.
 */
@Schema(description = "A historic decision instance.")
public record HistoricDecisionInstanceResultDto(
        @Schema(description = "The id of the historic decision instance.") String id,
        @Schema(description = "The id of the decision definition.") String decisionDefinitionId,
        @Schema(description = "The key of the decision definition.") String decisionDefinitionKey,
        @Schema(description = "The name of the decision definition.") String decisionDefinitionName,
        @Schema(description = "The time the decision was evaluated.") Date evaluationTime,
        @Schema(description = "The time this historic decision instance will be removed.") Date removalTime,
        @Schema(description = "The key of the process definition.") String processDefinitionKey,
        @Schema(description = "The id of the process definition.") String processDefinitionId,
        @Schema(description = "The id of the process instance.") String processInstanceId,
        @Schema(description = "The key of the case definition.") String caseDefinitionKey,
        @Schema(description = "The id of the case definition.") String caseDefinitionId,
        @Schema(description = "The id of the case instance.") String caseInstanceId,
        @Schema(description = "The id of the activity that triggered the decision.") String activityId,
        @Schema(description = "The id of the activity instance that triggered the decision.") String activityInstanceId,
        @Schema(description = "The id of the user who evaluated the decision.") String userId,
        @Schema(description = "The result of the collect operation for decisions using a collect hit policy.") Double collectResultValue,
        @Schema(description = "The id of the root decision instance.") String rootDecisionInstanceId,
        @Schema(description = "The id of the root process instance.") String rootProcessInstanceId,
        @Schema(description = "The id of the decision requirements definition.") String decisionRequirementsDefinitionId,
        @Schema(description = "The key of the decision requirements definition.") String decisionRequirementsDefinitionKey,
        @Schema(description = "The id of the tenant.") String tenantId
) {
    public static HistoricDecisionInstanceResultDto fromHistoricDecisionInstance(HistoricDecisionInstance historicDecisionInstance) {
        return new HistoricDecisionInstanceResultDto(
                historicDecisionInstance.getId(),
                historicDecisionInstance.getDecisionDefinitionId(),
                historicDecisionInstance.getDecisionDefinitionKey(),
                historicDecisionInstance.getDecisionDefinitionName(),
                historicDecisionInstance.getEvaluationTime(),
                historicDecisionInstance.getRemovalTime(),
                historicDecisionInstance.getProcessDefinitionKey(),
                historicDecisionInstance.getProcessDefinitionId(),
                historicDecisionInstance.getProcessInstanceId(),
                historicDecisionInstance.getCaseDefinitionKey(),
                historicDecisionInstance.getCaseDefinitionId(),
                historicDecisionInstance.getCaseInstanceId(),
                historicDecisionInstance.getActivityId(),
                historicDecisionInstance.getActivityInstanceId(),
                historicDecisionInstance.getUserId(),
                historicDecisionInstance.getCollectResultValue(),
                historicDecisionInstance.getRootDecisionInstanceId(),
                historicDecisionInstance.getRootProcessInstanceId(),
                historicDecisionInstance.getDecisionRequirementsDefinitionId(),
                historicDecisionInstance.getDecisionRequirementsDefinitionKey(),
                historicDecisionInstance.getTenantId()
        );
    }
}

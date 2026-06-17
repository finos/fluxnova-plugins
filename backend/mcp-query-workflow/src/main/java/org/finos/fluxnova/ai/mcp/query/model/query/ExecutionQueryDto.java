package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.ExecutionQuery;

import java.util.List;

/**
 * DTO for querying executions via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering executions.")
public record ExecutionQueryDto(
        @Schema(description = "Filter by the id of the execution.")
        String executionId,

        @Schema(description = "Filter by the id of the process instance the execution belongs to.")
        String processInstanceId,

        @Schema(description = "Filter by the business key of the process instances the executions belong to.")
        String businessKey,

        @Schema(description = "Filter by the id of the process definition the executions run on.")
        String processDefinitionId,

        @Schema(description = "Filter by the key of the process definition the executions run on.")
        String processDefinitionKey,

        @Schema(description = "Filter by the id of the activity the execution currently executes.")
        String activityId,

        @Schema(description = "Select only those executions that expect a signal of the given name.")
        String signalEventSubscriptionName,

        @Schema(description = "Select only those executions that expect a message of the given name.")
        String messageEventSubscriptionName,

        @Schema(description = "Only include active executions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include suspended executions. "
                + "Value may only be true, as false is the default behavior.")
        Boolean suspended,

        @Schema(description = "Filter by the incident id.")
        String incidentId,

        @Schema(description = "Filter by incident type.", allowableValues = {"failedJob", "failedExternalTask"})
        String incidentType,

        @Schema(description = "Filter by the incident message. Exact match.")
        String incidentMessage,

        @Schema(description = "Filter by the incident message that the parameter is a substring of.")
        String incidentMessageLike,

        @Schema(description = "Filter by a list of tenant ids. An execution must have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include executions which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Match all variable names case-insensitively when used with variable value filters.")
        Boolean variableNamesIgnoreCase,

        @Schema(description = "Match all variable values case-insensitively when used with variable value filters.")
        Boolean variableValuesIgnoreCase
) {
    public ExecutionQuery toQuery(RuntimeService runtimeService) {
        ExecutionQuery query = runtimeService.createExecutionQuery();
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (businessKey != null) {
            query.processInstanceBusinessKey(businessKey);
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (processDefinitionKey != null) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (activityId != null) {
            query.activityId(activityId);
        }
        if (signalEventSubscriptionName != null) {
            query.signalEventSubscriptionName(signalEventSubscriptionName);
        }
        if (messageEventSubscriptionName != null) {
            query.messageEventSubscriptionName(messageEventSubscriptionName);
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        if (incidentId != null) {
            query.incidentId(incidentId);
        }
        if (incidentType != null) {
            query.incidentType(incidentType);
        }
        if (incidentMessage != null) {
            query.incidentMessage(incidentMessage);
        }
        if (incidentMessageLike != null) {
            query.incidentMessageLike(incidentMessageLike);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(variableNamesIgnoreCase)) {
            query.matchVariableNamesIgnoreCase();
        }
        if (Boolean.TRUE.equals(variableValuesIgnoreCase)) {
            query.matchVariableValuesIgnoreCase();
        }
        return query;
    }
}

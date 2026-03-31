package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.EventSubscriptionQuery;

import java.util.List;

/**
 * DTO for querying event subscriptions via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering event subscriptions.")
public record EventSubscriptionQueryDto(
        @Schema(description = "Only select the subscription with the given id.")
        String eventSubscriptionId,

        @Schema(description = "Only select subscriptions for events with the given name.")
        String eventName,

        @Schema(description = "Only select subscriptions for events with the given type.",
                allowableValues = {"message", "signal", "compensate", "conditional"})
        String eventType,

        @Schema(description = "Only select subscriptions that belong to an execution with the given id.")
        String executionId,

        @Schema(description = "Only select subscriptions that belong to a process instance with the given id.")
        String processInstanceId,

        @Schema(description = "Only select subscriptions that belong to an activity with the given id.")
        String activityId,

        @Schema(description = "Filter by a list of tenant ids. "
                + "Only select subscriptions that belong to one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only select subscriptions which have no tenant id. "
                + "Value may only be true, as false is the default behavior.")
        Boolean withoutTenantId,

        @Schema(description = "Select event subscriptions which have no tenant id. "
                + "Can be used in combination with tenantIdIn. "
                + "Value may only be true, as false is the default behavior.")
        Boolean includeEventSubscriptionsWithoutTenantId
) {
    public EventSubscriptionQuery toQuery(RuntimeService runtimeService) {
        EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();
        if (eventSubscriptionId != null) {
            query.eventSubscriptionId(eventSubscriptionId);
        }
        if (eventName != null) {
            query.eventName(eventName);
        }
        if (eventType != null) {
            query.eventType(eventType);
        }
        if (executionId != null) {
            query.executionId(executionId);
        }
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (activityId != null) {
            query.activityId(activityId);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(includeEventSubscriptionsWithoutTenantId)) {
            query.includeEventSubscriptionsWithoutTenantId();
        }
        return query;
    }
}

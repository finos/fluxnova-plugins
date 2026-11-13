package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.runtime.EventSubscription;

import java.util.Date;

/**
 * Result DTO for event subscription query results.
 * Maps the fields from the engine's {@link EventSubscription} interface.
 */
public record EventSubscriptionResultDto(
        String id,
        String eventType,
        String eventName,
        String executionId,
        String processInstanceId,
        String activityId,
        String tenantId,
        Date createdDate
) {
    public static EventSubscriptionResultDto fromEventSubscription(EventSubscription eventSubscription) {
        return new EventSubscriptionResultDto(
                eventSubscription.getId(),
                eventSubscription.getEventType(),
                eventSubscription.getEventName(),
                eventSubscription.getExecutionId(),
                eventSubscription.getProcessInstanceId(),
                eventSubscription.getActivityId(),
                eventSubscription.getTenantId(),
                eventSubscription.getCreated()
        );
    }
}

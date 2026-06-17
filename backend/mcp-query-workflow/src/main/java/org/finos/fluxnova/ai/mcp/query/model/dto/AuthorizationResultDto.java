package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.authorization.Authorization;

/**
 * Result DTO for authorization query results.
 * Maps the fields from the engine's {@link Authorization} interface.
 */
public record AuthorizationResultDto(
        String id,
        int authorizationType,
        String userId,
        String groupId,
        int resourceType,
        String resourceId
) {
    public static AuthorizationResultDto fromAuthorization(Authorization authorization) {
        return new AuthorizationResultDto(
                authorization.getId(),
                authorization.getAuthorizationType(),
                authorization.getUserId(),
                authorization.getGroupId(),
                authorization.getResourceType(),
                authorization.getResourceId()
        );
    }
}

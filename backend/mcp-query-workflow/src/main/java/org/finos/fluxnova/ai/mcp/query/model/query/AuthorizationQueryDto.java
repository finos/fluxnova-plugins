package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.authorization.AuthorizationQuery;

import java.util.List;

/**
 * DTO for querying authorizations via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering authorizations.")
public record AuthorizationQueryDto(
        @Schema(description = "Filter by the id of the authorization.")
        String authorizationId,

        @Schema(description = "Filter by the type of the authorization. "
                + "Legal values are 0 (global), 1 (grant), 2 (revoke).")
        Integer authorizationType,

        @Schema(description = "Filter by a list of user ids. An authorization must apply to one of the given users.")
        List<String> userIdIn,

        @Schema(description = "Filter by a list of group ids. An authorization must apply to one of the given groups.")
        List<String> groupIdIn,

        @Schema(description = "Filter by the numeric resource type of the authorization. "
                + "See the engine's Resources enum for valid values, e.g., 6 for process-definition.")
        Integer resourceType,

        @Schema(description = "Filter by the resource id of the authorization. "
                + "Use * to match authorizations that apply to all instances of the resource type.")
        String resourceId
) {
    public AuthorizationQuery toQuery(AuthorizationService authorizationService) {
        AuthorizationQuery query = authorizationService.createAuthorizationQuery();
        if (authorizationId != null) {
            query.authorizationId(authorizationId);
        }
        if (authorizationType != null) {
            query.authorizationType(authorizationType);
        }
        if (userIdIn != null && !userIdIn.isEmpty()) {
            query.userIdIn(userIdIn.toArray(new String[0]));
        }
        if (groupIdIn != null && !groupIdIn.isEmpty()) {
            query.groupIdIn(groupIdIn.toArray(new String[0]));
        }
        if (resourceType != null) {
            query.resourceType(resourceType);
        }
        if (resourceId != null) {
            query.resourceId(resourceId);
        }
        return query;
    }
}

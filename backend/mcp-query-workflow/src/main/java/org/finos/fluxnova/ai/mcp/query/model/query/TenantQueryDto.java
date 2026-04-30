package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.identity.TenantQuery;

import java.util.List;

/**
 * DTO for querying tenants via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering tenants.")
public record TenantQueryDto(
        @Schema(description = "Filter by the id of the tenant.")
        String tenantId,

        @Schema(description = "Filter by a list of tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Filter by the name of the tenant.")
        String tenantName,

        @Schema(description = "Filter by tenants whose name matches the given pattern. "
                + "The syntax is that of SQL, e.g. %acme%.")
        String tenantNameLike,

        @Schema(description = "Filter by tenants where the given user is a member.")
        String userMember,

        @Schema(description = "Filter by tenants where the given group is a member.")
        String groupMember,

        @Schema(description = "If true, also includes tenants of groups the user is a member of. "
                + "Can only be used in combination with userMember.")
        Boolean includingGroupsOfUser
) {
    public TenantQuery toQuery(IdentityService identityService) {
        TenantQuery query = identityService.createTenantQuery();
        if (tenantId != null) {
            query.tenantId(tenantId);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (tenantName != null) {
            query.tenantName(tenantName);
        }
        if (tenantNameLike != null) {
            query.tenantNameLike(tenantNameLike);
        }
        if (userMember != null) {
            query.userMember(userMember);
        }
        if (groupMember != null) {
            query.groupMember(groupMember);
        }
        if (Boolean.TRUE.equals(includingGroupsOfUser)) {
            query.includingGroupsOfUser(true);
        }
        return query;
    }
}

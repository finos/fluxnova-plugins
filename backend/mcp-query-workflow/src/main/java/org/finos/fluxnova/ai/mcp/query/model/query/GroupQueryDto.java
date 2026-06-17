package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.identity.GroupQuery;

import java.util.List;

/**
 * DTO for querying groups via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering groups.")
public record GroupQueryDto(
        @Schema(description = "Filter by the id of the group.")
        String groupId,

        @Schema(description = "Filter by a list of group ids.")
        List<String> groupIdIn,

        @Schema(description = "Filter by the name of the group.")
        String groupName,

        @Schema(description = "Filter by groups whose name matches the given pattern. "
                + "The syntax is that of SQL, e.g. %admin%.")
        String groupNameLike,

        @Schema(description = "Filter by the type of the group.")
        String groupType,

        @Schema(description = "Filter by groups which have the given user as a member.")
        String groupMember,

        @Schema(description = "Filter by groups that belong to the given tenant.")
        String memberOfTenant
) {
    public GroupQuery toQuery(IdentityService identityService) {
        GroupQuery query = identityService.createGroupQuery();
        if (groupId != null) {
            query.groupId(groupId);
        }
        if (groupIdIn != null && !groupIdIn.isEmpty()) {
            query.groupIdIn(groupIdIn.toArray(new String[0]));
        }
        if (groupName != null) {
            query.groupName(groupName);
        }
        if (groupNameLike != null) {
            query.groupNameLike(groupNameLike);
        }
        if (groupType != null) {
            query.groupType(groupType);
        }
        if (groupMember != null) {
            query.groupMember(groupMember);
        }
        if (memberOfTenant != null) {
            query.memberOfTenant(memberOfTenant);
        }
        return query;
    }
}

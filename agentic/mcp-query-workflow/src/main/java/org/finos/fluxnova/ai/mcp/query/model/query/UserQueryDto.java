package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.identity.UserQuery;

import java.util.List;

/**
 * DTO for querying users via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering users.")
public record UserQueryDto(
        @Schema(description = "Filter by the id of the user.")
        String userId,

        @Schema(description = "Filter by a list of user ids.")
        List<String> userIdIn,

        @Schema(description = "Filter by the first name of the user.")
        String userFirstName,

        @Schema(description = "Filter by users whose first name matches the given pattern. "
                + "The syntax is that of SQL, e.g. %john%.")
        String userFirstNameLike,

        @Schema(description = "Filter by the last name of the user.")
        String userLastName,

        @Schema(description = "Filter by users whose last name matches the given pattern. "
                + "The syntax is that of SQL, e.g. %doe%.")
        String userLastNameLike,

        @Schema(description = "Filter by the email address of the user.")
        String userEmail,

        @Schema(description = "Filter by users whose email address matches the given pattern. "
                + "The syntax is that of SQL, e.g. %@example.com.")
        String userEmailLike,

        @Schema(description = "Filter by users who are a member of the given group.")
        String memberOfGroup,

        @Schema(description = "Filter by users who are a member of the given tenant.")
        String memberOfTenant
) {
    public UserQuery toQuery(IdentityService identityService) {
        UserQuery query = identityService.createUserQuery();
        if (userId != null) {
            query.userId(userId);
        }
        if (userIdIn != null && !userIdIn.isEmpty()) {
            query.userIdIn(userIdIn.toArray(new String[0]));
        }
        if (userFirstName != null) {
            query.userFirstName(userFirstName);
        }
        if (userFirstNameLike != null) {
            query.userFirstNameLike(userFirstNameLike);
        }
        if (userLastName != null) {
            query.userLastName(userLastName);
        }
        if (userLastNameLike != null) {
            query.userLastNameLike(userLastNameLike);
        }
        if (userEmail != null) {
            query.userEmail(userEmail);
        }
        if (userEmailLike != null) {
            query.userEmailLike(userEmailLike);
        }
        if (memberOfGroup != null) {
            query.memberOfGroup(memberOfGroup);
        }
        if (memberOfTenant != null) {
            query.memberOfTenant(memberOfTenant);
        }
        return query;
    }
}

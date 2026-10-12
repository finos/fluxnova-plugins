package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.identity.User;

/**
 * Result DTO representing a user.
 * Note: the user's password is intentionally omitted.
 */
@Schema(description = "Represents a user in the identity service.")
public record UserResultDto(
        @Schema(description = "The id of the user.")
        String id,

        @Schema(description = "The first name of the user.")
        String firstName,

        @Schema(description = "The last name of the user.")
        String lastName,

        @Schema(description = "The email address of the user.")
        String email
) {
    public static UserResultDto fromUser(User user) {
        return new UserResultDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}

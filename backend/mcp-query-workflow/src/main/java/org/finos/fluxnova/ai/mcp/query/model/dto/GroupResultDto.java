package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.identity.Group;

/**
 * Result DTO representing a group.
 */
@Schema(description = "Represents a group in the identity service.")
public record GroupResultDto(
        @Schema(description = "The id of the group.")
        String id,

        @Schema(description = "The name of the group.")
        String name,

        @Schema(description = "The type of the group.")
        String type
) {
    public static GroupResultDto fromGroup(Group group) {
        return new GroupResultDto(
                group.getId(),
                group.getName(),
                group.getType()
        );
    }
}

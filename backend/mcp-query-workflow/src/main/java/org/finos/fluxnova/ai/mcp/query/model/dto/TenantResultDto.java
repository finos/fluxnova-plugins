package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.identity.Tenant;

/**
 * Result DTO representing a tenant.
 */
@Schema(description = "Represents a tenant in the identity service.")
public record TenantResultDto(
        @Schema(description = "The id of the tenant.")
        String id,

        @Schema(description = "The name of the tenant.")
        String name
) {
    public static TenantResultDto fromTenant(Tenant tenant) {
        return new TenantResultDto(
                tenant.getId(),
                tenant.getName()
        );
    }
}

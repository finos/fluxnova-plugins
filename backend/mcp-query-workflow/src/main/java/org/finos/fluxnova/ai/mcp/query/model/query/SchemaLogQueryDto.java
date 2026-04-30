package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.management.SchemaLogQuery;

/**
 * DTO for querying schema log entries via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering schema log entries.")
public record SchemaLogQueryDto(
        @Schema(description = "Filter by the schema version.")
        String version
) {
    public SchemaLogQuery toQuery(ManagementService managementService) {
        SchemaLogQuery query = managementService.createSchemaLogQuery();
        if (version != null) {
            query.version(version);
        }
        return query;
    }
}

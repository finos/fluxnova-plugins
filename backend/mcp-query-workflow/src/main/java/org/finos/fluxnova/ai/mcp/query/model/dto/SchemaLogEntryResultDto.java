package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.management.SchemaLogEntry;

import java.util.Date;

/**
 * Result DTO representing a schema log entry.
 */
@Schema(description = "Represents an entry in the database schema version log.")
public record SchemaLogEntryResultDto(
        @Schema(description = "The unique identifier of the schema log entry.")
        String id,

        @Schema(description = "The date and time when this schema change was applied.")
        Date timestamp,

        @Schema(description = "The schema version of this log entry.")
        String version
) {
    public static SchemaLogEntryResultDto fromSchemaLogEntry(SchemaLogEntry schemaLogEntry) {
        return new SchemaLogEntryResultDto(
                schemaLogEntry.getId(),
                schemaLogEntry.getTimestamp(),
                schemaLogEntry.getVersion()
        );
    }
}

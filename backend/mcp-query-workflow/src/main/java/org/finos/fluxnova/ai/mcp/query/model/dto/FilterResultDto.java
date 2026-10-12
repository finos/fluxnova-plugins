package org.finos.fluxnova.ai.mcp.query.model.dto;

import org.finos.fluxnova.bpm.engine.filter.Filter;

/**
 * Result DTO for filter query results.
 * Maps the fields from the engine's {@link Filter} interface.
 */
public record FilterResultDto(
        String id,
        String resourceType,
        String name,
        String owner
) {
    public static FilterResultDto fromFilter(Filter filter) {
        return new FilterResultDto(
                filter.getId(),
                filter.getResourceType(),
                filter.getName(),
                filter.getOwner()
        );
    }
}

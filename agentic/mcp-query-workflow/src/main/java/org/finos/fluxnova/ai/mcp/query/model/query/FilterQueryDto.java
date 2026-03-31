package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.FilterService;
import org.finos.fluxnova.bpm.engine.filter.FilterQuery;

/**
 * DTO for querying filters via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering saved query filters.")
public record FilterQueryDto(
        @Schema(description = "Filter by the id of the filter.")
        String filterId,

        @Schema(description = "Filter by the resource type of the filter, e.g., 'Task'.")
        String resourceType,

        @Schema(description = "Filter by the name of the filter. Exact match.")
        String name,

        @Schema(description = "Filter by filter names that the parameter is a substring of. "
                + "The syntax is the same as in SQL, e.g., %name%.")
        String nameLike,

        @Schema(description = "Filter by the owner of the filter.")
        String owner
) {
    public FilterQuery toQuery(FilterService filterService) {
        FilterQuery query = filterService.createFilterQuery();
        if (filterId != null) {
            query.filterId(filterId);
        }
        if (resourceType != null) {
            query.filterResourceType(resourceType);
        }
        if (name != null) {
            query.filterName(name);
        }
        if (nameLike != null) {
            query.filterNameLike(nameLike);
        }
        if (owner != null) {
            query.filterOwner(owner);
        }
        return query;
    }
}

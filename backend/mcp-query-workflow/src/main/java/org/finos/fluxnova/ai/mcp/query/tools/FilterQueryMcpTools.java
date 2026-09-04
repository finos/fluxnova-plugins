package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.FilterResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.FilterQueryDto;
import org.finos.fluxnova.bpm.engine.FilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * MCP tools for querying filter data from the process engine.
 * <p>
 * Provides read-only query access to saved query filters
 * through the process engine's FilterService Query API.
 */
public class FilterQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(FilterQueryMcpTools.class);

    private final FilterService filterService;
    private final int defaultMaxResults;

    public FilterQueryMcpTools(FilterService filterService,
                               int defaultMaxResults) {
        this.filterService = filterService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Filter Query ----
    public List<FilterResultDto> queryFilters(
            FilterQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying filters with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(filterService) : filterService.createFilterQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<FilterResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(FilterResultDto::fromFilter)
                .toList();

        LOG.info("Filter query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

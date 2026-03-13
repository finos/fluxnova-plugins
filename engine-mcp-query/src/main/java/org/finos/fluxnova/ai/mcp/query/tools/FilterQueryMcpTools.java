package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.FilterResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.FilterQueryDto;
import org.finos.fluxnova.bpm.engine.FilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;

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
                               @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.filterService = filterService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Filter Query ----

    @McpTool(description = "Query saved query filters in the process engine. "
            + "Returns a list of filters matching the given filter criteria. "
            + "A filter is a saved query (e.g., a saved task query) that can be reused to retrieve "
            + "a predefined set of results. Filters have a resource type such as 'Task', a name, and an owner. "
            + "Use this tool to discover what saved filters exist and who owns them. "
            + "All filter parameters are optional.")
    public List<FilterResultDto> queryFilters(
            @McpToolParam FilterQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying filters with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<FilterResultDto> resultDtos = queryDto.toQuery(filterService).list().stream()
                .limit(limit)
                .map(FilterResultDto::fromFilter)
                .toList();

        LOG.info("Filter query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

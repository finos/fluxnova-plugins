package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.AuthorizationResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.AuthorizationQueryDto;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * MCP tools for querying authorization data from the process engine.
 * <p>
 * Provides read-only query access to authorizations
 * through the process engine's AuthorizationService Query API.
 */
public class AuthorizationQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationQueryMcpTools.class);

    private final AuthorizationService authorizationService;
    private final int defaultMaxResults;

    public AuthorizationQueryMcpTools(AuthorizationService authorizationService,
                                      @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.authorizationService = authorizationService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Authorization Query ----

    @McpTool(description = "Query authorizations in the process engine. "
            + "Returns a list of authorizations matching the given filter criteria. "
            + "An authorization assigns a set of permissions to a user or group for a specific resource. "
            + "There are three authorization types: global (0), grant (1), and revoke (2). "
            + "Use this tool to inspect which permissions users or groups have for specific resources. "
            + "All filter parameters are optional.")
    public List<AuthorizationResultDto> queryAuthorizations(
            @McpToolParam AuthorizationQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying authorizations with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<AuthorizationResultDto> resultDtos = queryDto.toQuery(authorizationService).list().stream()
                .limit(limit)
                .map(AuthorizationResultDto::fromAuthorization)
                .toList();

        LOG.info("Authorization query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

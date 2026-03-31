package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.AuthorizationResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.AuthorizationQueryDto;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                                      int defaultMaxResults) {
        this.authorizationService = authorizationService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Authorization Query ----
    public List<AuthorizationResultDto> queryAuthorizations(
            AuthorizationQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying authorizations with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(authorizationService) : authorizationService.createAuthorizationQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<AuthorizationResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(AuthorizationResultDto::fromAuthorization)
                .toList();

        LOG.info("Authorization query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

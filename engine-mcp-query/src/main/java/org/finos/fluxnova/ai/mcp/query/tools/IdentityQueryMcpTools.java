package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.GroupResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.TenantResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.UserResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.GroupQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.TenantQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.UserQueryDto;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * MCP tools for querying identity data from the process engine.
 * <p>
 * Provides read-only query access to users, groups, and tenants
 * through the process engine's IdentityService Query API.
 */
public class IdentityQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityQueryMcpTools.class);

    private final IdentityService identityService;
    private final int defaultMaxResults;

    public IdentityQueryMcpTools(IdentityService identityService,
                                 @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.identityService = identityService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- User Query ----

    @McpTool(description = "Query users in the process engine identity service. "
            + "Use this tool to find users by id, name, email, group membership, or tenant membership. "
            + "Passwords are never included in the results. "
            + "All filter parameters are optional.")
    public List<UserResultDto> queryUsers(
            @McpToolParam UserQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying users with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<UserResultDto> resultDtos = queryDto.toQuery(identityService).list().stream()
                .limit(limit)
                .map(UserResultDto::fromUser)
                .toList();

        LOG.info("User query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Group Query ----

    @McpTool(description = "Query groups in the process engine identity service. "
            + "Use this tool to find groups by id, name, type, member user, or tenant. "
            + "All filter parameters are optional.")
    public List<GroupResultDto> queryGroups(
            @McpToolParam GroupQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying groups with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<GroupResultDto> resultDtos = queryDto.toQuery(identityService).list().stream()
                .limit(limit)
                .map(GroupResultDto::fromGroup)
                .toList();

        LOG.info("Group query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Tenant Query ----

    @McpTool(description = "Query tenants in the process engine identity service. "
            + "Use this tool to find tenants by id, name, or by the users and groups that are members of them. "
            + "All filter parameters are optional.")
    public List<TenantResultDto> queryTenants(
            @McpToolParam TenantQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying tenants with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<TenantResultDto> resultDtos = queryDto.toQuery(identityService).list().stream()
                .limit(limit)
                .map(TenantResultDto::fromTenant)
                .toList();

        LOG.info("Tenant query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

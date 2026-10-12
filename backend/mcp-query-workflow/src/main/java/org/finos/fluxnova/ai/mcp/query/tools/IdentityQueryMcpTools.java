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
                                 int defaultMaxResults) {
        this.identityService = identityService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- User Query ----
    public List<UserResultDto> queryUsers(
            UserQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying users with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(identityService) : identityService.createUserQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<UserResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(UserResultDto::fromUser)
                .toList();

        LOG.info("User query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Group Query ----
    public List<GroupResultDto> queryGroups(
            GroupQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying groups with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(identityService) : identityService.createGroupQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<GroupResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(GroupResultDto::fromGroup)
                .toList();

        LOG.info("Group query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Tenant Query ----
    public List<TenantResultDto> queryTenants(
            TenantQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying tenants with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(identityService) : identityService.createTenantQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<TenantResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(TenantResultDto::fromTenant)
                .toList();

        LOG.info("Tenant query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

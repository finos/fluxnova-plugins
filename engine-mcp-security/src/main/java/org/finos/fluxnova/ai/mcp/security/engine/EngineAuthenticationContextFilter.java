package org.finos.fluxnova.ai.mcp.security.engine;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.Tenant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Per-request filter that propagates the Spring Security authentication into the
 * Fluxnova process engine's identity context.
 *
 * <p>After Spring Security has authenticated the request (via HTTP Basic Auth or OAuth2),
 * this filter reads the authenticated principal from the {@link SecurityContextHolder},
 * resolves the user's groups and tenants from the engine's identity service, and calls
 * {@link org.finos.fluxnova.bpm.engine.IdentityService#setAuthentication} so that the
 * engine's built-in authorization checks fire correctly for all MCP tool invocations.</p>
 *
 * <p>Authentication context is always cleared in a {@code finally} block to prevent
 * leaking identity across requests.</p>
 *
 * <p>This mirrors the behaviour of {@code ProcessEngineAuthenticationFilter} from
 * {@code engine-rest}, which performs the same {@code setAuthentication} /
 * {@code clearAuthentication} lifecycle but is activated via web.xml (commented out
 * in all current production deployments).</p>
 */
public class EngineAuthenticationContextFilter extends OncePerRequestFilter {

    private final ProcessEngine processEngine;

    public EngineAuthenticationContextFilter(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String userId = authentication.getName();
            try {
                List<String> groupIds = getGroupsOfUser(userId);
                List<String> tenantIds = getTenantsOfUser(userId);
                processEngine.getIdentityService().setAuthentication(userId, groupIds, tenantIds);
                filterChain.doFilter(request, response);
            } finally {
                processEngine.getIdentityService().clearAuthentication();
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private List<String> getGroupsOfUser(String userId) {
        return processEngine.getIdentityService()
                .createGroupQuery()
                .groupMember(userId)
                .list()
                .stream()
                .map(Group::getId)
                .collect(Collectors.toList());
    }

    private List<String> getTenantsOfUser(String userId) {
        return processEngine.getIdentityService()
                .createTenantQuery()
                .userMember(userId)
                .includingGroupsOfUser(true)
                .list()
                .stream()
                .map(Tenant::getId)
                .collect(Collectors.toList());
    }
}

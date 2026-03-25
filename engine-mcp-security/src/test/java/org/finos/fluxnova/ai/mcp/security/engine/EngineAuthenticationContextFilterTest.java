package org.finos.fluxnova.ai.mcp.security.engine;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.finos.fluxnova.ai.mcp.security.permissions.McpPermission;
import org.finos.fluxnova.ai.mcp.security.permissions.McpResource;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.GroupQuery;
import org.finos.fluxnova.bpm.engine.identity.Tenant;
import org.finos.fluxnova.bpm.engine.identity.TenantQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EngineAuthenticationContextFilter")
class EngineAuthenticationContextFilterTest {

    @Mock
    private ProcessEngine processEngine;

    @Mock
    private IdentityService identityService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private GroupQuery groupQuery;

    @Mock
    private TenantQuery tenantQuery;

    @Mock
    private FilterChain filterChain;

    private EngineAuthenticationContextFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        lenient().when(processEngine.getIdentityService()).thenReturn(identityService);
        lenient().when(processEngine.getAuthorizationService()).thenReturn(authorizationService);
        filter = new EngineAuthenticationContextFilter(processEngine);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setUpAuthentication(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setUpGroupQuery(List<Group> groups) {
        when(identityService.createGroupQuery()).thenReturn(groupQuery);
        when(groupQuery.groupMember(anyString())).thenReturn(groupQuery);
        when(groupQuery.list()).thenReturn(groups);
    }

    private void setUpTenantQuery(List<Tenant> tenants) {
        when(identityService.createTenantQuery()).thenReturn(tenantQuery);
        when(tenantQuery.userMember(anyString())).thenReturn(tenantQuery);
        when(tenantQuery.includingGroupsOfUser(true)).thenReturn(tenantQuery);
        when(tenantQuery.list()).thenReturn(tenants);
    }

    private Group mockGroup(String id) {
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(id);
        return group;
    }

    private Tenant mockTenant(String id) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(id);
        return tenant;
    }

    @Nested
    @DisplayName("when user is authenticated and authorized")
    class AuthenticatedAndAuthorized {

        @Test
        @DisplayName("should set engine authentication and proceed with filter chain")
        void setsAuthAndProceeds() throws ServletException, IOException {
            setUpAuthentication("admin");
            setUpGroupQuery(List.of(mockGroup("fluxnova-admin"), mockGroup("editors")));
            setUpTenantQuery(List.of(mockTenant("tenant-a")));
            when(authorizationService.isUserAuthorized(
                    eq("admin"), eq(List.of("fluxnova-admin", "editors")),
                    eq(McpPermission.ACCESS), eq(McpResource.MCP)))
                    .thenReturn(true);

            filter.doFilter(request, response, filterChain);

            verify(identityService).setAuthentication(
                    "admin",
                    List.of("fluxnova-admin", "editors"),
                    List.of("tenant-a")
            );
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should always clear engine authentication after request completes")
        void clearsAuthenticationAfterSuccess() throws ServletException, IOException {
            setUpAuthentication("admin");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(true);

            filter.doFilter(request, response, filterChain);

            InOrder inOrder = inOrder(identityService, filterChain);
            inOrder.verify(identityService).setAuthentication(anyString(), anyList(), anyList());
            inOrder.verify(filterChain).doFilter(request, response);
            inOrder.verify(identityService).clearAuthentication();
        }

        @Test
        @DisplayName("should handle user with no groups and no tenants")
        void noGroupsNoTenants() throws ServletException, IOException {
            setUpAuthentication("lonely-user");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized("lonely-user", Collections.emptyList(),
                    McpPermission.ACCESS, McpResource.MCP)).thenReturn(true);

            filter.doFilter(request, response, filterChain);

            verify(identityService).setAuthentication("lonely-user", Collections.emptyList(), Collections.emptyList());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should resolve multiple groups correctly")
        void multipleGroups() throws ServletException, IOException {
            setUpAuthentication("user1");
            setUpGroupQuery(List.of(mockGroup("g1"), mockGroup("g2"), mockGroup("g3")));
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(eq("user1"), eq(List.of("g1", "g2", "g3")),
                    eq(McpPermission.ACCESS), eq(McpResource.MCP))).thenReturn(true);

            filter.doFilter(request, response, filterChain);

            verify(identityService).setAuthentication("user1", List.of("g1", "g2", "g3"), Collections.emptyList());
        }

        @Test
        @DisplayName("should resolve multiple tenants correctly")
        void multipleTenants() throws ServletException, IOException {
            setUpAuthentication("user1");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(List.of(mockTenant("t1"), mockTenant("t2")));
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(true);

            filter.doFilter(request, response, filterChain);

            verify(identityService).setAuthentication("user1", Collections.emptyList(), List.of("t1", "t2"));
        }
    }

    @Nested
    @DisplayName("when user is authenticated but NOT authorized")
    class AuthenticatedButNotAuthorized {

        @Test
        @DisplayName("should return 403 Forbidden")
        void returns403() throws ServletException, IOException {
            setUpAuthentication("unauthorized-user");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        }

        @Test
        @DisplayName("should NOT proceed with filter chain")
        void doesNotProceed() throws ServletException, IOException {
            setUpAuthentication("unauthorized-user");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(false);

            filter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should include username in 403 error message")
        void forbiddenMessageContainsUsername() throws ServletException, IOException {
            setUpAuthentication("baduser");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertTrue(response.getErrorMessage().contains("baduser"),
                    "Error message should contain the username");
        }

        @Test
        @DisplayName("should still clear engine authentication on 403")
        void clearsAuthOn403() throws ServletException, IOException {
            setUpAuthentication("unauthorized-user");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(false);

            filter.doFilter(request, response, filterChain);

            verify(identityService).clearAuthentication();
        }
    }

    @Nested
    @DisplayName("when user is NOT authenticated")
    class NotAuthenticated {

        @Test
        @DisplayName("should proceed without setting engine authentication when no auth context")
        void noAuthContext_proceedsWithoutSetting() throws ServletException, IOException {
            // SecurityContextHolder has no authentication
            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(identityService, never()).setAuthentication(anyString(), anyList(), anyList());
            verify(identityService, never()).clearAuthentication();
        }

        @Test
        @DisplayName("should proceed without setting engine authentication when auth is not authenticated")
        void unauthenticatedAuth_proceedsWithoutSetting() throws ServletException, IOException {
            UsernamePasswordAuthenticationToken unauthed =
                    new UsernamePasswordAuthenticationToken("user", "pass");
            // By default, UsernamePasswordAuthenticationToken without authorities is NOT authenticated
            SecurityContextHolder.getContext().setAuthentication(unauthed);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(identityService, never()).setAuthentication(anyString(), anyList(), anyList());
        }
    }

    @Nested
    @DisplayName("cleanup on exception")
    class CleanupOnException {

        @Test
        @DisplayName("should clear engine authentication even when filter chain throws")
        void filterChainThrows_stillClearsAuth() throws ServletException, IOException {
            setUpAuthentication("admin");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(true);
            doThrow(new ServletException("downstream error")).when(filterChain).doFilter(any(), any());

            assertThrows(ServletException.class,
                    () -> filter.doFilter(request, response, filterChain));

            verify(identityService).clearAuthentication();
        }

        @Test
        @DisplayName("should clear engine authentication even when filter chain throws RuntimeException")
        void filterChainThrowsRuntime_stillClearsAuth() throws ServletException, IOException {
            setUpAuthentication("admin");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(true);
            doThrow(new RuntimeException("unexpected")).when(filterChain).doFilter(any(), any());

            assertThrows(RuntimeException.class,
                    () -> filter.doFilter(request, response, filterChain));

            verify(identityService).clearAuthentication();
        }

        @Test
        @DisplayName("should clear engine authentication even when group query throws")
        void groupQueryThrows_stillClearsAuth() throws ServletException, IOException {
            setUpAuthentication("admin");
            when(identityService.createGroupQuery()).thenReturn(groupQuery);
            when(groupQuery.groupMember(anyString())).thenReturn(groupQuery);
            when(groupQuery.list()).thenThrow(new RuntimeException("group query failed"));

            assertThrows(RuntimeException.class,
                    () -> filter.doFilter(request, response, filterChain));

            verify(identityService).clearAuthentication();
        }
    }

    @Nested
    @DisplayName("authorization check")
    class AuthorizationCheck {

        @Test
        @DisplayName("should check MCP ACCESS permission with correct user and groups")
        void checksCorrectPermission() throws ServletException, IOException {
            setUpAuthentication("user1");
            setUpGroupQuery(List.of(mockGroup("grp-a"), mockGroup("grp-b")));
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(true);

            filter.doFilter(request, response, filterChain);

            verify(authorizationService).isUserAuthorized(
                    "user1",
                    List.of("grp-a", "grp-b"),
                    McpPermission.ACCESS,
                    McpResource.MCP
            );
        }

        @Test
        @DisplayName("should use tenant query with includingGroupsOfUser=true")
        void tenantQueryIncludesGroupUsers() throws ServletException, IOException {
            setUpAuthentication("user1");
            setUpGroupQuery(Collections.emptyList());
            setUpTenantQuery(Collections.emptyList());
            when(authorizationService.isUserAuthorized(anyString(), anyList(), any(), any()))
                    .thenReturn(true);

            filter.doFilter(request, response, filterChain);

            verify(tenantQuery).userMember("user1");
            verify(tenantQuery).includingGroupsOfUser(true);
        }
    }
}

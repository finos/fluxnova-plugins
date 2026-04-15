package org.finos.fluxnova.ai.mcp.security.integration;

import org.finos.fluxnova.ai.mcp.security.permissions.McpPermission;
import org.finos.fluxnova.ai.mcp.security.permissions.McpResource;
import org.finos.fluxnova.ai.mcp.security.securityconfigs.SecurityConfig;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.GroupQuery;
import org.finos.fluxnova.bpm.engine.identity.Tenant;
import org.finos.fluxnova.bpm.engine.identity.TenantQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests that verify the complete security filter chain behaviour
 * using a real Spring Security context with MockMvc.
 */
@WebMvcTest
@ContextConfiguration(classes = McpSecurityFilterChainIT.TestConfig.class)
@DisplayName("MCP Security Filter Chain Integration")
class McpSecurityFilterChainIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessEngine processEngine;

    private static String basicAuth(String user, String pass) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
    }

    private void stubIdentityForUser(String userId, boolean validPassword,
                                     List<String> groupIds, List<String> tenantIds,
                                     boolean mcpAuthorized) {
        IdentityService identityService = processEngine.getIdentityService();
        AuthorizationService authorizationService = processEngine.getAuthorizationService();

        // Reset mocks
        reset(identityService, authorizationService);

        when(identityService.checkPassword(eq(userId), anyString())).thenReturn(validPassword);

        // Group query chain
        GroupQuery groupQuery = mock(GroupQuery.class);
        when(identityService.createGroupQuery()).thenReturn(groupQuery);
        when(groupQuery.groupMember(userId)).thenReturn(groupQuery);
        List<Group> groups = groupIds.stream().map(id -> {
            Group g = mock(Group.class);
            when(g.getId()).thenReturn(id);
            return g;
        }).toList();
        when(groupQuery.list()).thenReturn(groups);

        // Tenant query chain
        TenantQuery tenantQuery = mock(TenantQuery.class);
        when(identityService.createTenantQuery()).thenReturn(tenantQuery);
        when(tenantQuery.userMember(userId)).thenReturn(tenantQuery);
        when(tenantQuery.includingGroupsOfUser(true)).thenReturn(tenantQuery);
        List<Tenant> tenants = tenantIds.stream().map(id -> {
            Tenant t = mock(Tenant.class);
            when(t.getId()).thenReturn(id);
            return t;
        }).toList();
        when(tenantQuery.list()).thenReturn(tenants);

        // Authorization check
        when(authorizationService.isUserAuthorized(eq(userId), anyList(),
                eq(McpPermission.ACCESS), eq(McpResource.MCP)))
                .thenReturn(mcpAuthorized);
    }

    @Nested
    @DisplayName("GET /mcp/test")
    class McpEndpoint {

        @Test
        @DisplayName("should return 401 when no credentials provided")
        void noCredentials_returns401() throws Exception {
            mockMvc.perform(get("/mcp/test"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 401 when invalid credentials provided")
        void invalidCredentials_returns401() throws Exception {
            IdentityService identityService = processEngine.getIdentityService();
            reset(identityService);
            when(identityService.checkPassword("admin", "wrong")).thenReturn(false);

            mockMvc.perform(get("/mcp/test")
                            .header("Authorization", basicAuth("admin", "wrong")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 when authenticated but not authorized for MCP")
        void authenticatedButNotAuthorized_returns403() throws Exception {
            stubIdentityForUser("viewer", true,
                    List.of("viewers"), Collections.emptyList(), false);

            mockMvc.perform(get("/mcp/test")
                            .header("Authorization", basicAuth("viewer", "pass")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 when authenticated and authorized")
        void authenticatedAndAuthorized_returns200() throws Exception {
            stubIdentityForUser("admin", true,
                    List.of("fluxnova-admin"), Collections.emptyList(), true);

            mockMvc.perform(get("/mcp/test")
                            .header("Authorization", basicAuth("admin", "pass")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("mcp-ok"));
        }

        @Test
        @DisplayName("should set engine authentication context for authorized request")
        void authorizedRequest_setsEngineAuthContext() throws Exception {
            stubIdentityForUser("admin", true,
                    List.of("fluxnova-admin", "developers"), List.of("tenant-1"), true);

            mockMvc.perform(get("/mcp/test")
                    .header("Authorization", basicAuth("admin", "pass")));

            IdentityService identityService = processEngine.getIdentityService();
            verify(identityService).setAuthentication(
                    "admin",
                    List.of("fluxnova-admin", "developers"),
                    List.of("tenant-1")
            );
        }

        @Test
        @DisplayName("should clear engine authentication context after request")
        void clearsEngineAuth() throws Exception {
            stubIdentityForUser("admin", true,
                    List.of("fluxnova-admin"), Collections.emptyList(), true);

            mockMvc.perform(get("/mcp/test")
                    .header("Authorization", basicAuth("admin", "pass")));

            verify(processEngine.getIdentityService()).clearAuthentication();
        }

    }

    @Nested
    @DisplayName("GET /sse/test")
    class SseEndpoint {

        @Test
        @DisplayName("should return 401 when no credentials provided")
        void noCredentials_returns401() throws Exception {
            mockMvc.perform(get("/sse/test"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 200 when authenticated and authorized")
        void authenticatedAndAuthorized_returns200() throws Exception {
            stubIdentityForUser("admin", true,
                    List.of("fluxnova-admin"), Collections.emptyList(), true);

            mockMvc.perform(get("/sse/test")
                            .header("Authorization", basicAuth("admin", "pass")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("sse-ok"));
        }
    }

    @Nested
    @DisplayName("non-MCP endpoints")
    class NonMcpEndpoints {

        @Test
        @DisplayName("should not be secured by this filter chain (no auth required)")
        void nonMcpPath_noAuthRequired() throws Exception {
            mockMvc.perform(get("/api/other"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("other-ok"));
        }
    }

    @Nested
    @DisplayName("CSRF handling")
    class CsrfHandling {

        @Test
        @DisplayName("should allow POST to /mcp/** without CSRF token (CSRF disabled)")
        void postWithoutCsrf_allowed() throws Exception {
            stubIdentityForUser("admin", true,
                    List.of("fluxnova-admin"), Collections.emptyList(), true);

            mockMvc.perform(post("/mcp/action")
                            .header("Authorization", basicAuth("admin", "pass"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("mcp-post-ok"));
        }
    }

    @Nested
    @DisplayName("session management")
    class SessionManagement {

        @Test
        @DisplayName("should not create HTTP session (stateless)")
        void noSession() throws Exception {
            stubIdentityForUser("admin", true,
                    List.of("fluxnova-admin"), Collections.emptyList(), true);

            mockMvc.perform(get("/mcp/test")
                            .header("Authorization", basicAuth("admin", "pass")))
                    .andExpect(request -> assertNull(
                            request.getRequest().getSession(false),
                            "No HTTP session should be created (stateless mode)"));
        }
    }

    @Configuration
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        ProcessEngine processEngine() {
            ProcessEngine engine = mock(ProcessEngine.class);
            IdentityService identityService = mock(IdentityService.class);
            AuthorizationService authorizationService = mock(AuthorizationService.class);
            when(engine.getIdentityService()).thenReturn(identityService);
            when(engine.getAuthorizationService()).thenReturn(authorizationService);
            return engine;
        }

        @Bean
        TestMcpController testMcpController() {
            return new TestMcpController();
        }

        @Bean
        TestSseController testSseController() {
            return new TestSseController();
        }

        @Bean
        TestOtherController testOtherController() {
            return new TestOtherController();
        }
    }

    @RestController
    static class TestMcpController {
        @GetMapping("/mcp/test")
        public String mcpTest() {
            return "mcp-ok";
        }

        @org.springframework.web.bind.annotation.PostMapping("/mcp/action")
        public String mcpAction() {
            return "mcp-post-ok";
        }
    }

    @RestController
    static class TestSseController {
        @GetMapping("/sse/test")
        public String sseTest() {
            return "sse-ok";
        }
    }

    @RestController
    static class TestOtherController {
        @GetMapping("/api/other")
        public String otherEndpoint() {
            return "other-ok";
        }
    }
}

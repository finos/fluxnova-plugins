package org.finos.fluxnova.ai.mcp.security.integration;

import org.finos.fluxnova.ai.mcp.security.permissions.McpPermission;
import org.finos.fluxnova.ai.mcp.security.permissions.McpPermissionProvider;
import org.finos.fluxnova.ai.mcp.security.permissions.McpResource;
import org.finos.fluxnova.ai.mcp.security.permissions.McpSecurityEnginePlugin;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.authorization.Authorization;
import org.finos.fluxnova.bpm.engine.authorization.AuthorizationQuery;
import org.finos.fluxnova.bpm.engine.authorization.Groups;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.finos.fluxnova.bpm.engine.impl.util.ResourceTypeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test that exercises the full McpSecurityEnginePlugin lifecycle:
 * preInit + postProcessEngineBuild as it would happen in a real engine boot sequence.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("McpSecurityEnginePlugin Lifecycle Integration")
class EnginePluginLifecycleIT {

    @Mock
    private ProcessEngineConfigurationImpl configuration;

    @Mock
    private ProcessEngine processEngine;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private AuthorizationQuery authorizationQuery;

    private McpSecurityEnginePlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new McpSecurityEnginePlugin();
    }

    @Test
    @DisplayName("full lifecycle: preInit registers provider, postBuild creates authorization")
    void fullLifecycle_registersAndCreatesAuth() {
        // Phase 1: preInit
        plugin.preInit(configuration);

        // Verify provider is registered
        ArgumentCaptor<McpPermissionProvider> providerCaptor =
                ArgumentCaptor.forClass(McpPermissionProvider.class);
        verify(configuration).setPermissionProvider(providerCaptor.capture());
        McpPermissionProvider registeredProvider = providerCaptor.getValue();

        // Verify the registered provider can resolve MCP permissions
        assertEquals(McpPermission.ACCESS,
                registeredProvider.getPermissionForName("ACCESS", McpResource.MCP.resourceType()));
        assertEquals(McpPermission.NONE,
                registeredProvider.getPermissionForName("NONE", McpResource.MCP.resourceType()));

        // Verify resource type registration
        assertSame(McpPermission.class,
                ResourceTypeUtil.getPermissionEnums().get(McpResource.MCP.resourceType()));

        // Phase 2: postProcessEngineBuild
        when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
        when(configuration.isAuthorizationEnabled()).thenReturn(true);
        when(processEngine.getAuthorizationService()).thenReturn(authorizationService);
        when(authorizationService.createAuthorizationQuery()).thenReturn(authorizationQuery);
        when(authorizationQuery.groupIdIn(anyString())).thenReturn(authorizationQuery);
        when(authorizationQuery.resourceType(any(McpResource.class))).thenReturn(authorizationQuery);
        when(authorizationQuery.resourceId(anyString())).thenReturn(authorizationQuery);
        when(authorizationQuery.count()).thenReturn(0L);

        plugin.postProcessEngineBuild(processEngine);

        // Verify authorization was created properly
        ArgumentCaptor<AuthorizationEntity> authCaptor =
                ArgumentCaptor.forClass(AuthorizationEntity.class);
        verify(authorizationService).saveAuthorization(authCaptor.capture());

        AuthorizationEntity created = authCaptor.getValue();
        assertEquals(Authorization.AUTH_TYPE_GRANT, created.getAuthorizationType());
        assertEquals(Groups.CAMUNDA_ADMIN, created.getGroupId());
        assertEquals(McpResource.MCP.resourceType(), created.getResourceType());
        assertEquals(Authorization.ANY, created.getResourceId());
    }

    @Test
    @DisplayName("full lifecycle: preInit then postBuild with auth disabled skips authorization")
    void lifecycle_authDisabled_skipsAuthorization() {
        // Phase 1
        plugin.preInit(configuration);

        // Phase 2
        when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
        when(configuration.isAuthorizationEnabled()).thenReturn(false);

        plugin.postProcessEngineBuild(processEngine);

        verify(authorizationService, never()).saveAuthorization(any());
    }

    @Test
    @DisplayName("full lifecycle: idempotent — existing authorization prevents duplicate")
    void lifecycle_idempotent() {
        // Phase 1
        plugin.preInit(configuration);

        // Phase 2 — first time
        when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
        when(configuration.isAuthorizationEnabled()).thenReturn(true);
        when(processEngine.getAuthorizationService()).thenReturn(authorizationService);
        when(authorizationService.createAuthorizationQuery()).thenReturn(authorizationQuery);
        when(authorizationQuery.groupIdIn(anyString())).thenReturn(authorizationQuery);
        when(authorizationQuery.resourceType(any(McpResource.class))).thenReturn(authorizationQuery);
        when(authorizationQuery.resourceId(anyString())).thenReturn(authorizationQuery);

        // Simulate: authorization already exists
        when(authorizationQuery.count()).thenReturn(1L);

        plugin.postProcessEngineBuild(processEngine);

        verify(authorizationService, never()).saveAuthorization(any());
    }
}

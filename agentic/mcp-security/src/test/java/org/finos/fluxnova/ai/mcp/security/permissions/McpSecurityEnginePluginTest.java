package org.finos.fluxnova.ai.mcp.security.permissions;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpSecurityEnginePlugin")
class McpSecurityEnginePluginTest {

    private McpSecurityEnginePlugin plugin;

    @Mock
    private ProcessEngineConfigurationImpl configuration;

    @Mock
    private ProcessEngine processEngine;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private AuthorizationQuery authorizationQuery;

    @BeforeEach
    void setUp() {
        plugin = new McpSecurityEnginePlugin();
    }

    @Nested
    @DisplayName("preInit()")
    class PreInit {

        @Test
        @DisplayName("should set McpPermissionProvider on configuration")
        void setsPermissionProvider() {
            plugin.preInit(configuration);

            ArgumentCaptor<McpPermissionProvider> captor =
                    ArgumentCaptor.forClass(McpPermissionProvider.class);
            verify(configuration).setPermissionProvider(captor.capture());
            assertInstanceOf(McpPermissionProvider.class, captor.getValue());
        }

        @Test
        @DisplayName("should register McpPermission enum for resource type 22")
        void registersPermissionEnum() {
            plugin.preInit(configuration);

            Class<?> registered = ResourceTypeUtil.getPermissionEnums()
                    .get(McpResource.MCP.resourceType());
            assertSame(McpPermission.class, registered,
                    "McpPermission enum should be registered for resource type 22");
        }
    }

    @Nested
    @DisplayName("postProcessEngineBuild()")
    class PostProcessEngineBuild {

        @BeforeEach
        void setUpMocks() {
            lenient().when(processEngine.getAuthorizationService()).thenReturn(authorizationService);
            lenient().when(authorizationService.createAuthorizationQuery()).thenReturn(authorizationQuery);
            lenient().when(authorizationQuery.groupIdIn(anyString())).thenReturn(authorizationQuery);
            lenient().when(authorizationQuery.resourceType(any(McpResource.class))).thenReturn(authorizationQuery);
            lenient().when(authorizationQuery.resourceId(anyString())).thenReturn(authorizationQuery);
        }

        @Test
        @DisplayName("should skip when authorization is not enabled")
        void authorizationDisabled_skips() {
            when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
            when(configuration.isAuthorizationEnabled()).thenReturn(false);

            plugin.postProcessEngineBuild(processEngine);

            verify(authorizationService, never()).saveAuthorization(any());
            verify(authorizationService, never()).createAuthorizationQuery();
        }

        @Test
        @DisplayName("should create admin authorization when none exists")
        void noExistingAuth_createsAdminAuth() {
            when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
            when(configuration.isAuthorizationEnabled()).thenReturn(true);
            when(authorizationQuery.count()).thenReturn(0L);

            plugin.postProcessEngineBuild(processEngine);

            ArgumentCaptor<AuthorizationEntity> captor =
                    ArgumentCaptor.forClass(AuthorizationEntity.class);
            verify(authorizationService).saveAuthorization(captor.capture());

            AuthorizationEntity saved = captor.getValue();
            assertEquals(Groups.CAMUNDA_ADMIN, saved.getGroupId());
            assertEquals(McpResource.MCP.resourceType(), saved.getResourceType());
            assertEquals(Authorization.ANY, saved.getResourceId());
            assertEquals(Authorization.AUTH_TYPE_GRANT, saved.getAuthorizationType());
        }

        @Test
        @DisplayName("should query for existing admin authorization correctly")
        void queriesAuthorizationCorrectly() {
            when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
            when(configuration.isAuthorizationEnabled()).thenReturn(true);
            when(authorizationQuery.count()).thenReturn(0L);

            plugin.postProcessEngineBuild(processEngine);

            verify(authorizationQuery).groupIdIn(Groups.CAMUNDA_ADMIN);
            verify(authorizationQuery).resourceType(McpResource.MCP);
            verify(authorizationQuery).resourceId(Authorization.ANY);
        }

        @Test
        @DisplayName("should NOT create authorization when one already exists")
        void existingAuth_doesNotCreate() {
            when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
            when(configuration.isAuthorizationEnabled()).thenReturn(true);
            when(authorizationQuery.count()).thenReturn(1L);

            plugin.postProcessEngineBuild(processEngine);

            verify(authorizationService, never()).saveAuthorization(any());
        }

        @Test
        @DisplayName("should NOT create authorization when multiple already exist")
        void multipleExistingAuth_doesNotCreate() {
            when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
            when(configuration.isAuthorizationEnabled()).thenReturn(true);
            when(authorizationQuery.count()).thenReturn(5L);

            plugin.postProcessEngineBuild(processEngine);

            verify(authorizationService, never()).saveAuthorization(any());
        }

        @Test
        @DisplayName("created authorization should grant ACCESS permission")
        void createdAuth_grantsAccessPermission() {
            when(processEngine.getProcessEngineConfiguration()).thenReturn(configuration);
            when(configuration.isAuthorizationEnabled()).thenReturn(true);
            when(authorizationQuery.count()).thenReturn(0L);

            plugin.postProcessEngineBuild(processEngine);

            ArgumentCaptor<AuthorizationEntity> captor =
                    ArgumentCaptor.forClass(AuthorizationEntity.class);
            verify(authorizationService).saveAuthorization(captor.capture());

            AuthorizationEntity saved = captor.getValue();
            // Verify ACCESS was added (the permission bits should include ACCESS value)
            assertTrue((saved.getPermissions() & McpPermission.ACCESS.getValue()) == McpPermission.ACCESS.getValue(),
                    "Authorization should grant ACCESS permission");
        }
    }
}

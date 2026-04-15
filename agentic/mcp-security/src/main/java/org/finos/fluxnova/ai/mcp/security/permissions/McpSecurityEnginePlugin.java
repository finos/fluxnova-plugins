package org.finos.fluxnova.ai.mcp.security.permissions;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.authorization.Authorization;
import org.finos.fluxnova.bpm.engine.authorization.Groups;
import org.finos.fluxnova.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.finos.fluxnova.bpm.engine.impl.util.ResourceTypeUtil;

public class McpSecurityEnginePlugin extends AbstractProcessEnginePlugin {

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        configuration.setPermissionProvider(new McpPermissionProvider());

        // Register the MCP permission enum so the REST API can resolve resource type 22
        ResourceTypeUtil.getPermissionEnums()
                .put(McpResource.MCP.resourceType(), McpPermission.class);
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
        if (!processEngine.getProcessEngineConfiguration().isAuthorizationEnabled()) {
            return;
        }

        AuthorizationService authorizationService = processEngine.getAuthorizationService();

        if (authorizationService.createAuthorizationQuery()
                .groupIdIn(Groups.CAMUNDA_ADMIN)
                .resourceType(McpResource.MCP)
                .resourceId(Authorization.ANY)
                .count() == 0) {
            AuthorizationEntity mcpAuth = new AuthorizationEntity(Authorization.AUTH_TYPE_GRANT);
            mcpAuth.setGroupId(Groups.CAMUNDA_ADMIN);
            mcpAuth.setResource(McpResource.MCP);
            mcpAuth.setResourceId(Authorization.ANY);
            mcpAuth.addPermission(McpPermission.ACCESS);
            authorizationService.saveAuthorization(mcpAuth);
        }
    }
}

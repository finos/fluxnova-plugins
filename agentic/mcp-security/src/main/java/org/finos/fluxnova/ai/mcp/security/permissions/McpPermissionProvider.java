package org.finos.fluxnova.ai.mcp.security.permissions;

import org.finos.fluxnova.bpm.engine.authorization.Permission;
import org.finos.fluxnova.bpm.engine.impl.cfg.auth.DefaultPermissionProvider;

public class McpPermissionProvider extends DefaultPermissionProvider {

    @Override
    public Permission getPermissionForName(String name, int resourceType) {
        if (resourceType == McpResource.MCP.resourceType()) {
            for (McpPermission p : McpPermission.values()) {
                if (p.getName().equals(name)) return p;
            }
        }
        return super.getPermissionForName(name, resourceType);
    }

    @Override
    public Permission[] getPermissionsForResource(int resourceType) {
        if (resourceType == McpResource.MCP.resourceType()) {
            return McpPermission.values();
        }
        return super.getPermissionsForResource(resourceType);
    }

    @Override
    public String getNameForResource(int resourceType) {
        if (resourceType == McpResource.MCP.resourceType()) {
            return McpResource.MCP.resourceName();
        }
        return super.getNameForResource(resourceType);
    }
}

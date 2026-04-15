package org.finos.fluxnova.ai.mcp.security.permissions;

import org.finos.fluxnova.bpm.engine.authorization.Permission;
import org.finos.fluxnova.bpm.engine.authorization.Resource;

public enum McpPermission implements Permission {
    NONE("NONE", 0),
    ACCESS("ACCESS", Integer.MAX_VALUE);

    private static final Resource[] RESOURCES = new Resource[] { McpResource.MCP };

    private String name;
    private int id;

    private McpPermission(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getValue() {
        return id;
    }

    @Override
    public Resource[] getTypes() {
        return RESOURCES;
    }
}

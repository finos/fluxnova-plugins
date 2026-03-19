package org.finos.fluxnova.ai.mcp.security.permissions;

import org.finos.fluxnova.bpm.engine.authorization.Resource;

public enum McpResource implements Resource {
    MCP("MCP", 22);

    String name;
    int id;

    McpResource(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String resourceName() {
        return name;
    }

    public int resourceType() {
        return id;
    }
}

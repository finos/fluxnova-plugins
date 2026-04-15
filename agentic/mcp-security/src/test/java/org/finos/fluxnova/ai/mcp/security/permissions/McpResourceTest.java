package org.finos.fluxnova.ai.mcp.security.permissions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("McpResource")
class McpResourceTest {

    @Test
    @DisplayName("MCP resource should have type id 22")
    void resourceType() {
        assertEquals(22, McpResource.MCP.resourceType());
    }

    @Test
    @DisplayName("valueOf should resolve correctly")
    void valueOf() {
        assertSame(McpResource.MCP, McpResource.valueOf("MCP"));
    }
}

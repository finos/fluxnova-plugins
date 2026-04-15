package org.finos.fluxnova.ai.mcp.security.permissions;

import org.finos.fluxnova.bpm.engine.authorization.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("McpPermission")
class McpPermissionTest {

    @Test
    @DisplayName("ACCESS should have value Integer.MAX_VALUE")
    void access_hasValueMaxInt() {
        // NOTE: The Permission contract states getValue() should return a power of 2.
        // Using Integer.MAX_VALUE makes ACCESS equivalent to the built-in Permissions.ALL,
        // which means granting ACCESS sets ALL permission bits. This is fine for the
        // current implementation of this resource, which only has a binary 
        // access/no-access model, but would prevent adding
        // more granular permissions (e.g. READ_TOOLS, CALL_TOOLS) in the future since
        // ACCESS would implicitly grant them all.
        // Consider using a single power-of-2 value (e.g., 2) if finer-grained
        // permissions may be needed later.
        assertEquals(Integer.MAX_VALUE, McpPermission.ACCESS.getValue());
    }

    @Test
    @DisplayName("both permissions should reference McpResource.MCP")
    void permissions_referenceCorrectResourceType() {
        for (McpPermission perm : McpPermission.values()) {
            Resource[] types = perm.getTypes();
            assertEquals(1, types.length, "Each permission should link to exactly one resource");
            assertSame(McpResource.MCP, types[0]);
        }
    }

    @Test
    @DisplayName("enum should have exactly two values")
    void enumHasTwoValues() {
        assertEquals(2, McpPermission.values().length);
    }

    @Test
    @DisplayName("valueOf should resolve correctly")
    void valueOf_resolvesCorrectly() {
        assertSame(McpPermission.NONE, McpPermission.valueOf("NONE"));
        assertSame(McpPermission.ACCESS, McpPermission.valueOf("ACCESS"));
    }
}

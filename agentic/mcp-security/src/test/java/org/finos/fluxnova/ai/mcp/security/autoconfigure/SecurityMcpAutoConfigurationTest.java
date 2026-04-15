package org.finos.fluxnova.ai.mcp.security.autoconfigure;

import org.finos.fluxnova.ai.mcp.security.permissions.McpSecurityEnginePlugin;
import org.finos.fluxnova.ai.mcp.security.securityconfigs.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityMcpAutoConfiguration")
class SecurityMcpAutoConfigurationTest {

    private SecurityMcpAutoConfiguration autoConfiguration;

    @BeforeEach
    void setUp() {
        autoConfiguration = new SecurityMcpAutoConfiguration();
    }

    @Test
    @DisplayName("mcpSecurityEnginePlugin should return a non-null McpSecurityEnginePlugin")
    void mcpSecurityEnginePlugin_returnsNonNull() {
        McpSecurityEnginePlugin plugin = autoConfiguration.mcpSecurityEnginePlugin();

        assertNotNull(plugin);
    }

    @Test
    @DisplayName("mcpSecurityEnginePlugin should return a McpSecurityEnginePlugin instance")
    void mcpSecurityEnginePlugin_returnsCorrectType() {
        Object plugin = autoConfiguration.mcpSecurityEnginePlugin();

        assertInstanceOf(McpSecurityEnginePlugin.class, plugin);
    }

    @Test
    @DisplayName("mcpSecurityEnginePlugin should return a new instance on each call")
    void mcpSecurityEnginePlugin_returnsNewInstanceEachCall() {
        McpSecurityEnginePlugin first = autoConfiguration.mcpSecurityEnginePlugin();
        McpSecurityEnginePlugin second = autoConfiguration.mcpSecurityEnginePlugin();

        assertNotSame(first, second);
    }

    @Test
    @DisplayName("class should be annotated with @AutoConfigureAfter pointing to FluxnovaBpmAutoConfiguration")
    void class_hasAutoConfigureAfterAnnotation() {
        AutoConfigureAfter annotation = SecurityMcpAutoConfiguration.class
                .getAnnotation(AutoConfigureAfter.class);

        assertNotNull(annotation, "@AutoConfigureAfter annotation must be present");
        assertTrue(
                Arrays.asList(annotation.name())
                        .contains("org.finos.fluxnova.bpm.spring.boot.starter.FluxnovaBpmAutoConfiguration"),
                "@AutoConfigureAfter must reference FluxnovaBpmAutoConfiguration"
        );
    }

    @Test
    @DisplayName("class should be annotated with @Import for SecurityConfig")
    void class_importsSecurityConfig() {
        Import annotation = SecurityMcpAutoConfiguration.class.getAnnotation(Import.class);

        assertNotNull(annotation, "@Import annotation must be present");
        assertTrue(
                Arrays.asList(annotation.value()).contains(SecurityConfig.class),
                "@Import must include SecurityConfig"
        );
    }
}

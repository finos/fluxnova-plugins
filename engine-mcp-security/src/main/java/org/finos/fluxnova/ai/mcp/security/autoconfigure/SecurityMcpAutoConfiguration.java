package org.finos.fluxnova.ai.mcp.security.autoconfigure;

import org.finos.fluxnova.ai.mcp.security.permissions.McpSecurityEnginePlugin;
import org.finos.fluxnova.ai.mcp.security.securityconfigs.SecurityConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter(name = "org.finos.fluxnova.bpm.spring.boot.starter.FluxnovaBpmAutoConfiguration")
@Import(SecurityConfig.class)
public class SecurityMcpAutoConfiguration {

    @Bean
    public McpSecurityEnginePlugin mcpSecurityEnginePlugin() {
        return new McpSecurityEnginePlugin();
    }
}

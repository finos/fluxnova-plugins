package org.finos.fluxnova.ai.mcp.security.autoconfigure;

import org.finos.fluxnova.ai.mcp.security.permissions.McpSecurityEnginePlugin;
import org.finos.fluxnova.ai.mcp.security.securityconfigs.SecurityConfig;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SecurityMcpAutoConfiguration")
class SecurityMcpAutoConfigurationIT {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    WebMvcAutoConfiguration.class,
                    SecurityAutoConfiguration.class,
                    SecurityMcpAutoConfiguration.class))
            .withUserConfiguration(MockProcessEngineConfig.class);

    @Test
    @DisplayName("should register McpSecurityEnginePlugin bean")
    void registersEnginePluginBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(McpSecurityEnginePlugin.class);
        });
    }

    @Test
    @DisplayName("should import SecurityConfig")
    void importsSecurityConfig() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityConfig.class);
        });
    }

    @Test
    @DisplayName("McpSecurityEnginePlugin bean should be a new instance")
    void pluginBeanIsNewInstance() {
        contextRunner.run(context -> {
            McpSecurityEnginePlugin plugin = context.getBean(McpSecurityEnginePlugin.class);
            assertThat(plugin).isNotNull();
        });
    }

    @Configuration
    @EnableWebSecurity
    static class MockProcessEngineConfig {
        @Bean
        ProcessEngine processEngine() {
            ProcessEngine engine = mock(ProcessEngine.class);
            IdentityService identityService = mock(IdentityService.class);
            AuthorizationService authorizationService = mock(AuthorizationService.class);
            when(engine.getIdentityService()).thenReturn(identityService);
            when(engine.getAuthorizationService()).thenReturn(authorizationService);
            return engine;
        }
    }
}

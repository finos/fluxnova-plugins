package org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.service.AdHocActivityToolInvocationServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnBean(RuntimeService.class)
public class AgentToolInvocationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ToolInvocationService toolInvocationService(RuntimeService runtimeService) {
        return new AdHocActivityToolInvocationServiceImpl(runtimeService);
    }
}

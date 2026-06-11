package org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure;

import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.lifecycle.AgentConfigUndeployListener;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AgentConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentConfigExtractor agentConfigExtractor() {
        return new AgentConfigExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentConfigRegistry agentConfigRegistry(AgentConfigExtractor extractor) {
        return new AgentConfigRegistry(extractor);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentConfigUndeployListener agentConfigUndeployListener(AgentConfigRegistry registry) {
        return new AgentConfigUndeployListener(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentConfigEnginePlugin agentConfigEnginePlugin() {
        return new AgentConfigEnginePlugin();
    }
}

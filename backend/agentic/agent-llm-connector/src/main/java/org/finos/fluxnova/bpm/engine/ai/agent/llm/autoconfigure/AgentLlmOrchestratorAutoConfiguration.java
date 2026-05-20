package org.finos.fluxnova.bpm.engine.ai.agent.llm.autoconfigure;

import org.finos.fluxnova.bpm.engine.ai.agent.llm.parser.AgentProviderEnginePlugin;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderProperties;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistryConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.LlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.SpringAiLlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.tool.AgentToolSchemaConverter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ChatModel.class)
@EnableConfigurationProperties(AgentProviderProperties.class)
public class AgentLlmOrchestratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentProviderRegistry agentProviderRegistry(ListableBeanFactory beanFactory,
                                                       AgentProviderProperties properties) {
        return AgentProviderRegistryConfig.build(beanFactory, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentToolSchemaConverter agentToolSchemaConverter() {
        return new AgentToolSchemaConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentProviderEnginePlugin agentProviderEnginePlugin(AgentProviderRegistry registry) {
        return new AgentProviderEnginePlugin(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmService llmService(AgentProviderRegistry registry,
                                 AgentToolSchemaConverter converter) {
        return new SpringAiLlmService(registry, converter);
    }
}

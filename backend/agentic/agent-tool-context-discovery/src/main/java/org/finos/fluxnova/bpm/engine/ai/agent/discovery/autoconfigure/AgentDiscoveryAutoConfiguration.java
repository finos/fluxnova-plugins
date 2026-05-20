package org.finos.fluxnova.bpm.engine.ai.agent.discovery.autoconfigure;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure.AgentConfigAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AdHocSubProcessCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.BpmnExtensionContextSpecBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.lifecycle.AgentDiscoveryUndeployListener;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.AgentContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = AgentConfigAutoConfiguration.class)
@ConditionalOnBean(RepositoryService.class)
public class AgentDiscoveryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentToolCatalogueBuilder agentToolCatalogueBuilder() {
        return new AdHocSubProcessCatalogueBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentContextSpecBuilder agentContextSpecExtractor() {
        return new BpmnExtensionContextSpecBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentToolCatalogueRegistry agentToolCatalogueRegistry(RepositoryService repositoryService,
                                                                  AgentConfigRegistry agentConfigRegistry,
                                                                  AgentToolCatalogueBuilder catalogueBuilder) {
        return new AgentToolCatalogueRegistry(repositoryService, agentConfigRegistry, catalogueBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentContextSpecRegistry agentContextSpecRegistry(RepositoryService repositoryService,
                                                              AgentConfigRegistry agentConfigRegistry,
                                                              AgentContextSpecBuilder extractor) {
        return new AgentContextSpecRegistry(repositoryService, agentConfigRegistry, extractor);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentContextResolver agentContextResolver(RuntimeService runtimeService) {
        return new AgentContextResolver(runtimeService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentDiscoveryUndeployListener agentDiscoveryUndeployListener(
            AgentToolCatalogueRegistry catalogueRegistry,
            AgentContextSpecRegistry contextSpecRegistry) {
        return new AgentDiscoveryUndeployListener(catalogueRegistry, contextSpecRegistry);
    }
}

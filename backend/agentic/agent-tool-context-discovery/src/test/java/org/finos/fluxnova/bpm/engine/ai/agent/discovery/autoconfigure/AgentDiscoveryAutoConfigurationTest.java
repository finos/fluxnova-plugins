package org.finos.fluxnova.bpm.engine.ai.agent.discovery.autoconfigure;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AdHocSubProcessCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.lifecycle.AgentDiscoveryUndeployListener;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.ContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentDiscoveryAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
    }

    @Configuration
    static class MockInfrastructure {
        @Bean RepositoryService repositoryService() { return mock(RepositoryService.class); }
        @Bean RuntimeService runtimeService() { return mock(RuntimeService.class); }
        @Bean AgentConfigExtractor agentConfigExtractor() { return new AgentConfigExtractor(); }
        @Bean AgentConfigRegistry agentConfigRegistry(RepositoryService rs, AgentConfigExtractor ext) {
            return new AgentConfigRegistry(rs, ext);
        }
    }

    @Test
    void contextLoads_allBeansPresent() {
        context = new AnnotationConfigApplicationContext(MockInfrastructure.class, AgentDiscoveryAutoConfiguration.class);

        assertNotNull(context.getBean(AgentToolCatalogueBuilder.class));
        assertNotNull(context.getBean(AgentContextSpecExtractor.class));
        assertNotNull(context.getBean(AgentToolCatalogueRegistry.class));
        assertNotNull(context.getBean(AgentContextSpecRegistry.class));
        assertNotNull(context.getBean(ContextResolver.class));
        assertNotNull(context.getBean(AgentDiscoveryUndeployListener.class));
    }

    @Test
    void defaultCatalogueBuilder_isAdHocSubProcessImpl() {
        context = new AnnotationConfigApplicationContext(MockInfrastructure.class, AgentDiscoveryAutoConfiguration.class);

        AgentToolCatalogueBuilder builder = context.getBean(AgentToolCatalogueBuilder.class);
        assertInstanceOf(AdHocSubProcessCatalogueBuilder.class, builder);
    }

    @Configuration
    static class CustomBuilderOverride {
        @Bean RepositoryService repositoryService() { return mock(RepositoryService.class); }
        @Bean RuntimeService runtimeService() { return mock(RuntimeService.class); }
        @Bean AgentConfigExtractor agentConfigExtractor() { return new AgentConfigExtractor(); }
        @Bean AgentConfigRegistry agentConfigRegistry(RepositoryService rs, AgentConfigExtractor ext) {
            return new AgentConfigRegistry(rs, ext);
        }
        @Bean AgentToolCatalogueBuilder agentToolCatalogueBuilder() {
            return mock(AgentToolCatalogueBuilder.class);
        }
    }

    @Test
    void conditionalOnMissingBean_allowsConsumerToOverrideCatalogueBuilder() {
        context = new AnnotationConfigApplicationContext(CustomBuilderOverride.class, AgentDiscoveryAutoConfiguration.class);

        AgentToolCatalogueBuilder builder = context.getBean(AgentToolCatalogueBuilder.class);
        assertFalse(builder instanceof AdHocSubProcessCatalogueBuilder);
    }
}

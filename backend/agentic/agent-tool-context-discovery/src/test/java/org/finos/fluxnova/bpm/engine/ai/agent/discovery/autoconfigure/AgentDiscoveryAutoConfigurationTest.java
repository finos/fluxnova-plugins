package org.finos.fluxnova.bpm.engine.ai.agent.discovery.autoconfigure;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AdHocSubProcessCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.BpmnExtensionContextSpecBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.lifecycle.AgentDiscoveryUndeployListener;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.AgentContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.impl.RepositoryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentDiscoveryAutoConfigurationTest {

    @Configuration
    static class MockInfrastructure {
        @Bean
        RepositoryServiceImpl repositoryService() {
            return mock(RepositoryServiceImpl.class);
        }

        @Bean
        RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        AgentConfigExtractor agentConfigExtractor() {
            return new AgentConfigExtractor();
        }

        @Bean
        AgentConfigRegistry agentConfigRegistry(ObjectProvider<RepositoryService> rs, AgentConfigExtractor ext) {
            return new AgentConfigRegistry(rs, ext);
        }
    }

    @Configuration
    static class CustomCatalogueBuilderOverride {
        @Bean
        RepositoryServiceImpl repositoryService() {
            return mock(RepositoryServiceImpl.class);
        }

        @Bean
        RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        AgentConfigExtractor agentConfigExtractor() {
            return new AgentConfigExtractor();
        }

        @Bean
        AgentConfigRegistry agentConfigRegistry(ObjectProvider<RepositoryService> rs, AgentConfigExtractor ext) {
            return new AgentConfigRegistry(rs, ext);
        }

        @Bean
        AgentToolCatalogueBuilder agentToolCatalogueBuilder() {
            return mock(AgentToolCatalogueBuilder.class);
        }
    }

    @Configuration
    static class CustomContextSpecBuilderOverride {
        @Bean
        RepositoryServiceImpl repositoryService() {
            return mock(RepositoryServiceImpl.class);
        }

        @Bean
        RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        AgentConfigExtractor agentConfigExtractor() {
            return new AgentConfigExtractor();
        }

        @Bean
        AgentConfigRegistry agentConfigRegistry(ObjectProvider<RepositoryService> rs, AgentConfigExtractor ext) {
            return new AgentConfigRegistry(rs, ext);
        }

        @Bean
        AgentContextSpecBuilder agentContextSpecExtractor() {
            return mock(AgentContextSpecBuilder.class);
        }
    }

    @Nested
    class DefaultBeans {

        private AnnotationConfigApplicationContext context;

        @BeforeEach
        void setUp() {
            context = new AnnotationConfigApplicationContext(
                    MockInfrastructure.class, AgentDiscoveryAutoConfiguration.class);
        }

        @AfterEach
        void tearDown() {
            context.close();
        }

        @Test
        void allExpectedBeansArePresent() {
            assertNotNull(context.getBean(AgentToolCatalogueBuilder.class));
            assertNotNull(context.getBean(AgentContextSpecBuilder.class));
            assertNotNull(context.getBean(AgentToolCatalogueRegistry.class));
            assertNotNull(context.getBean(AgentContextSpecRegistry.class));
            assertNotNull(context.getBean(AgentContextResolver.class));
            assertNotNull(context.getBean(AgentDiscoveryUndeployListener.class));
        }

        @Test
        void catalogueBuilder_isAdHocSubProcessImpl() {
            assertInstanceOf(AdHocSubProcessCatalogueBuilder.class,
                    context.getBean(AgentToolCatalogueBuilder.class));
        }

        @Test
        void contextSpecBuilder_isBpmnExtensionImpl() {
            assertInstanceOf(BpmnExtensionContextSpecBuilder.class,
                    context.getBean(AgentContextSpecBuilder.class));
        }
    }

    @Nested
    class ConditionalOnMissingBean {

        private AnnotationConfigApplicationContext context;

        @AfterEach
        void tearDown() {
            if (context != null) context.close();
        }

        @Test
        void userProvidedCatalogueBuilder_takesPreference() {
            context = new AnnotationConfigApplicationContext(
                    CustomCatalogueBuilderOverride.class, AgentDiscoveryAutoConfiguration.class);

            assertFalse(context.getBean(AgentToolCatalogueBuilder.class)
                    instanceof AdHocSubProcessCatalogueBuilder);
        }

        @Test
        void userProvidedContextSpecBuilder_takesPreference() {
            context = new AnnotationConfigApplicationContext(
                    CustomContextSpecBuilderOverride.class, AgentDiscoveryAutoConfiguration.class);

            assertFalse(context.getBean(AgentContextSpecBuilder.class)
                    instanceof BpmnExtensionContextSpecBuilder);
        }
    }
}
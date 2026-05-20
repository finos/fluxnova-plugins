package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.autoconfigure;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.AgentContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.LlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AdHocAgentOrchestrationParseListener;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AgentOrchestratorEnginePlugin;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AgentSubprocessEntryListener;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.SubprocessToolCompletionListener;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.AgentOrchestrationJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AdHocSubprocessTerminator;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AgentTerminationHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentOrchestratorAutoConfigurationTest {

    @Configuration
    static class MockInfrastructure {
        @Bean
        RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        LlmService llmService() {
            return mock(LlmService.class);
        }

        @Bean
        ToolInvocationService toolInvocationService() {
            return mock(ToolInvocationService.class);
        }

        @Bean
        AgentConfigRegistry agentConfigRegistry() {
            return mock(AgentConfigRegistry.class);
        }

        @Bean
        AgentToolCatalogueRegistry agentToolCatalogueRegistry() {
            return mock(AgentToolCatalogueRegistry.class);
        }

        @Bean
        AgentContextSpecRegistry agentContextSpecRegistry() {
            return mock(AgentContextSpecRegistry.class);
        }

        @Bean
        AgentContextResolver agentContextResolver() {
            return mock(AgentContextResolver.class);
        }
    }

    @Configuration
    static class CustomScopeCompleterOverride {
        @Bean
        RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        LlmService llmService() {
            return mock(LlmService.class);
        }

        @Bean
        ToolInvocationService toolInvocationService() {
            return mock(ToolInvocationService.class);
        }

        @Bean
        AgentConfigRegistry agentConfigRegistry() {
            return mock(AgentConfigRegistry.class);
        }

        @Bean
        AgentToolCatalogueRegistry agentToolCatalogueRegistry() {
            return mock(AgentToolCatalogueRegistry.class);
        }

        @Bean
        AgentContextSpecRegistry agentContextSpecRegistry() {
            return mock(AgentContextSpecRegistry.class);
        }

        @Bean
        AgentContextResolver agentContextResolver() {
            return mock(AgentContextResolver.class);
        }

        @Bean
        AgentTerminationHandler agentTerminationHandler() {
            return mock(AgentTerminationHandler.class);
        }
    }

    @Configuration
    static class MissingLlmService {
        @Bean
        RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        ToolInvocationService toolInvocationService() {
            return mock(ToolInvocationService.class);
        }
    }

    @Test
    void autoConfiguration_withAllDependencies_registersExpectedBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                MockInfrastructure.class, AgentOrchestratorAutoConfiguration.class)) {
            assertNotNull(context.getBean(AgentStateManager.class));
            assertNotNull(context.getBean(AgentSubprocessEntryListener.class));
            assertNotNull(context.getBean(SubprocessToolCompletionListener.class));
            assertInstanceOf(AdHocSubprocessTerminator.class,
                    context.getBean(AgentTerminationHandler.class));
            assertNotNull(context.getBean(AgentOrchestrationJobHandler.class));
            assertNotNull(context.getBean(AdHocAgentOrchestrationParseListener.class));
            assertNotNull(context.getBean(AgentOrchestratorEnginePlugin.class));
        }
    }

    @Test
    void autoConfiguration_whenTerminationHandlerProvided_usesUserBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                CustomScopeCompleterOverride.class, AgentOrchestratorAutoConfiguration.class)) {
            assertFalse(context
                    .getBean(AgentTerminationHandler.class) instanceof AdHocSubprocessTerminator);
        }
    }

    @Test
    void autoConfiguration_whenLlmServiceAbsent_doesNotActivate() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                MissingLlmService.class, AgentOrchestratorAutoConfiguration.class)) {
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(AgentOrchestratorEnginePlugin.class));
        }
    }
}

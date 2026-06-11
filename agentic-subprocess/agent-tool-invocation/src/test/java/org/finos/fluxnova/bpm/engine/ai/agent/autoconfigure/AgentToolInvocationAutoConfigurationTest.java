package org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.service.AdHocActivityToolInvocationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentToolInvocationAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
    }

    @Configuration
    static class MockInfrastructure {
        @Bean RepositoryService repositoryService() { return mock(RepositoryService.class); }
        @Bean RuntimeService runtimeService() { return mock(RuntimeService.class); }
    }

    @Test
    void contextLoads_allBeansPresent() {
        context = new AnnotationConfigApplicationContext(MockInfrastructure.class, AgentToolInvocationAutoConfiguration.class);

        assertNotNull(context.getBean(ToolInvocationService.class));
    }

    @Test
    void defaultToolInvocationService_isToolInvocationServiceImpl() {
        context = new AnnotationConfigApplicationContext(MockInfrastructure.class, AgentToolInvocationAutoConfiguration.class);

        ToolInvocationService service = context.getBean(ToolInvocationService.class);
        assertInstanceOf(AdHocActivityToolInvocationServiceImpl.class, service);
    }

    @Configuration
    static class CustomBuilderOverride {
        @Bean RepositoryService repositoryService() { return mock(RepositoryService.class); }
        @Bean RuntimeService runtimeService() { return mock(RuntimeService.class); }
        @Bean ToolInvocationService toolInvocationService() {
            return mock(ToolInvocationService.class);
        }
    }

    @Test
    void conditionalOnMissingBean_allowsConsumerToOverrideToolInvocationService() {
        context = new AnnotationConfigApplicationContext(CustomBuilderOverride.class, AgentToolInvocationAutoConfiguration.class);

        ToolInvocationService service = context.getBean(ToolInvocationService.class);
        assertFalse(service instanceof AdHocActivityToolInvocationServiceImpl);
    }
}

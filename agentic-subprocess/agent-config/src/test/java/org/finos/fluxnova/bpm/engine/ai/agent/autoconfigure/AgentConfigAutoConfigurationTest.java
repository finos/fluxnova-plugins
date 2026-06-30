package org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure;

import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.lifecycle.AgentConfigUndeployListener;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AgentConfigAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
    }

    @Test
    void contextLoads_allBeansPresent() {
        context = new AnnotationConfigApplicationContext(AgentConfigAutoConfiguration.class);

        assertNotNull(context.getBean(AgentConfigExtractor.class));
        assertNotNull(context.getBean(AgentConfigRegistry.class));
        assertNotNull(context.getBean(AgentConfigUndeployListener.class));
        assertNotNull(context.getBean(AgentConfigEnginePlugin.class));
    }

    @Configuration
    static class CustomExtractorOverride {
        static final AgentConfigExtractor CUSTOM = new AgentConfigExtractor();

        @Bean
        AgentConfigExtractor agentConfigExtractor() {
            return CUSTOM;
        }
    }

    @Test
    void conditionalOnMissingBean_allowsConsumerToOverrideExtractor() {
        context = new AnnotationConfigApplicationContext(CustomExtractorOverride.class, AgentConfigAutoConfiguration.class);

        assertSame(CustomExtractorOverride.CUSTOM, context.getBean(AgentConfigExtractor.class));
    }
}

package org.finos.fluxnova.bpm.engine.ai.agent.llm.autoconfigure;

import org.finos.fluxnova.bpm.engine.ai.agent.llm.parser.AgentProviderEnginePlugin;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.LlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.tool.AgentToolSchemaConverter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentLlmOrchestratorAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AgentLlmOrchestratorAutoConfiguration.class));

    @Test
    void contextLoads_whenChatModelBeanPresent_registersAllCoreBeans() {
        runner.withUserConfiguration(ChatModelConfig.class).run(context -> {
            assertThat(context).hasSingleBean(AgentProviderRegistry.class);
            assertThat(context).hasSingleBean(AgentToolSchemaConverter.class);
            assertThat(context).hasSingleBean(LlmService.class);

            AgentProviderRegistry registry = context.getBean(AgentProviderRegistry.class);
            assertThat(registry.has("ollama")).isTrue();
        });
    }

    @Test
    void contextLoads_whenChatModelBeanPresent_registersEnginePlugin() {
        runner.withUserConfiguration(ChatModelConfig.class).run(context -> {
            assertThat(context).hasSingleBean(AgentProviderEnginePlugin.class);
        });
    }

    @Test
    void contextLoads_whenNoChatModelBean_contextStartsWithEmptyProviderRegistry() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AgentProviderRegistry.class);
            assertThat(context).hasSingleBean(LlmService.class);

            AgentProviderRegistry registry = context.getBean(AgentProviderRegistry.class);
            assertThat(registry.has("anything")).isFalse();
        });
    }

    @Test
    void contextLoads_whenChatModelComesFromAnotherAutoConfiguration_registryResolvesProvider() {
        runner.withConfiguration(AutoConfigurations.of(SimulatedProviderAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasSingleBean(AgentProviderRegistry.class);
                assertThat(context).hasSingleBean(LlmService.class);

                AgentProviderRegistry registry = context.getBean(AgentProviderRegistry.class);
                assertThat(registry.has("openai")).isTrue();
            });
    }

    @Configuration
    static class ChatModelConfig {
        @Bean
        ChatModel ollamaChatModel() {
            return mock(ChatModel.class);
        }
    }

    @AutoConfiguration
    static class SimulatedProviderAutoConfiguration {
        @Bean
        ChatModel openaiChatModel() {
            return mock(ChatModel.class);
        }
    }
}

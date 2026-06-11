package org.finos.fluxnova.bpm.engine.ai.agent.llm.provider;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentProviderRegistryConfigTest {

    @Test
    void buildRegistry_whenBeanNameEndsChatModel_derivesProviderIdFromPrefix() {
        ChatModel ollama = mock(ChatModel.class);
        ChatModel huggingface = mock(ChatModel.class);

        Map<String, ChatModel> result = AgentProviderRegistryConfig.buildRegistry(
            Map.of("ollamaChatModel", ollama, "huggingfaceChatModel", huggingface),
            new AgentProviderProperties());

        assertThat(result.get("ollama")).isSameAs(ollama);
        assertThat(result.get("huggingface")).isSameAs(huggingface);
    }

    @Test
    void buildRegistry_whenBeanNameOmitsChatModelSuffix_beanIsExcluded() {
        ChatModel embedding = mock(ChatModel.class);

        Map<String, ChatModel> result = AgentProviderRegistryConfig.buildRegistry(
            Map.of("ollamaEmbeddingModel", embedding),
            new AgentProviderProperties());

        assertThat(result).doesNotContainKey("ollamaembedding");
    }

    @Test
    void buildRegistry_whenOverrideTargetsSameId_overrideTakesPrecedenceOverAutoDiscovery() {
        ChatModel autoBean = mock(ChatModel.class);
        ChatModel customBean = mock(ChatModel.class);

        Map<String, ChatModel> beans = new LinkedHashMap<>();
        beans.put("ollamaChatModel", autoBean);
        beans.put("customOllamaBean", customBean);

        AgentProviderProperties props = new AgentProviderProperties();
        Map<String, String> overrides = new HashMap<>();
        overrides.put("ollama", "customOllamaBean");
        props.setProviderOverrides(overrides);

        Map<String, ChatModel> result = AgentProviderRegistryConfig.buildRegistry(beans, props);

        assertThat(result.get("ollama")).isSameAs(customBean);
    }

    @Test
    void buildRegistry_whenOverrideReferencesCustomBeanName_registersUnderCustomProviderId() {
        ChatModel custom = mock(ChatModel.class);
        AgentProviderProperties props = new AgentProviderProperties();
        props.setProviderOverrides(Map.of("my-provider", "myCustomBean"));

        Map<String, ChatModel> result = AgentProviderRegistryConfig.buildRegistry(
            Map.of("myCustomBean", custom),
            props);

        assertThat(result.get("my-provider")).isSameAs(custom);
    }
}

package org.finos.fluxnova.bpm.engine.ai.agent.llm.provider;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AgentProviderRegistryConfig {

    private static final String CHAT_MODEL_SUFFIX = "ChatModel";

    private AgentProviderRegistryConfig() {}

    public static AgentProviderRegistry build(ListableBeanFactory beanFactory,
                                              AgentProviderProperties properties) {
        return new AgentProviderRegistry(() -> {
            Map<String, ChatModel> chatModels = beanFactory.getBeansOfType(ChatModel.class);
            return buildRegistry(chatModels, properties);
        });
    }

    static Map<String, ChatModel> buildRegistry(Map<String, ChatModel> chatModels,
                                                 AgentProviderProperties properties) {
        Map<String, ChatModel> registry = new HashMap<>();

        chatModels.forEach((beanName, model) -> {
            if (beanName.endsWith(CHAT_MODEL_SUFFIX) && beanName.length() > CHAT_MODEL_SUFFIX.length()) {
                String providerId = beanName
                    .substring(0, beanName.length() - CHAT_MODEL_SUFFIX.length())
                    .toLowerCase(Locale.ROOT);
                registry.put(providerId, model);
            }
        });

        properties.getProviderOverrides().forEach((providerId, beanName) -> {
            ChatModel model = chatModels.get(beanName);
            if (model != null) {
                registry.put(providerId, model);
            }
        });

        return registry;
    }
}

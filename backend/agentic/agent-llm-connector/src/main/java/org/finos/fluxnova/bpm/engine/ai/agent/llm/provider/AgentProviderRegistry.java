package org.finos.fluxnova.bpm.engine.ai.agent.llm.provider;

import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;
import java.util.function.Supplier;

public class AgentProviderRegistry {

    private final Supplier<Map<String, ChatModel>> supplier;
    private volatile Map<String, ChatModel> registry;

    public AgentProviderRegistry(Supplier<Map<String, ChatModel>> supplier) {
        this.supplier = supplier;
    }

    public ChatModel get(String providerId) {
        ChatModel model = resolve().get(providerId);
        if (model == null) {
            throw new IllegalStateException(
                "No ChatModel configured for provider '" + providerId + "'. " +
                "Ensure the Spring AI starter for this provider is on the classpath " +
                "and credentials are configured, or add an explicit override via " +
                "fluxnova.ai.agent.provider-overrides.");
        }
        return model;
    }

    public boolean has(String providerId) {
        return resolve().containsKey(providerId);
    }

    private Map<String, ChatModel> resolve() {
        if (registry == null) {
            registry = Map.copyOf(supplier.get());
        }
        return registry;
    }
}

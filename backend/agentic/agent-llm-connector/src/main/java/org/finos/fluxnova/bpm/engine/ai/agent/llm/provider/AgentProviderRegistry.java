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
        Map<String, ChatModel> current = registry;
        if (current == null) {
            current = Map.copyOf(supplier.get());
            // Only cache a non-empty result. If the supplier is called before the
            // Spring context has finished initialising (e.g. during pre-deployed
            // process re-parsing in DeploymentCache), getBeansOfType returns an
            // empty map. Caching that empty map would permanently hide all
            // providers from later callers. By refusing to cache empty results we
            // allow the next call — made once the context is fully ready — to
            // resolve the real provider beans.
            if (!current.isEmpty()) {
                registry = current;
            }
        }
        return current;
    }
}

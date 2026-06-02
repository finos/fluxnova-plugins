package org.finos.fluxnova.bpm.engine.ai.agent.llm.provider;

import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Resolves Spring AI {@link ChatModel} instances by provider id.
 *
 * <p>Provider ids are derived automatically from the bean names of {@link ChatModel}
 * beans on the classpath: a bean named {@code ollamaChatModel} maps to provider id
 * {@code "ollama"}. Explicit overrides can be declared via
 * {@link AgentProviderProperties#getProviderOverrides()} to handle non-standard bean
 * names or to remap a standard provider to a different bean instance.
 *
 * <p>The resolved registry is cached after the first successful lookup of a non-empty
 * bean map. If the Spring context has not yet fully initialised when {@code get} is
 * first called, the registry is not cached so that a subsequent call can resolve the
 * real beans.
 *
 * @see AgentProviderProperties
 * @see AgentProviderRegistryConfig
 */
public class AgentProviderRegistry {

    private final Supplier<Map<String, ChatModel>> supplier;
    private volatile Map<String, ChatModel> registry;

    public AgentProviderRegistry(Supplier<Map<String, ChatModel>> supplier) {
        this.supplier = supplier;
    }

    /**
     * Returns the {@link ChatModel} for the given provider id.
     *
     * @param providerId the provider id (e.g. {@code "ollama"}, {@code "openai"})
     * @return the configured {@link ChatModel} for that provider
     * @throws IllegalStateException if no {@link ChatModel} bean is mapped to
     *                               {@code providerId}
     */
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

    /**
     * Returns {@code true} if a {@link ChatModel} is mapped to the given provider id.
     *
     * @param providerId the provider id to check
     * @return {@code true} if the provider is available, {@code false} otherwise
     */
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

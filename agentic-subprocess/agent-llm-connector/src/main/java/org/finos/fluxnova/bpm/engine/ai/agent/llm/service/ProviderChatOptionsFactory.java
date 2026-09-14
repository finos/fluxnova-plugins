package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Strategy interface for building provider-specific {@link ChatOptions} at request time.
 *
 * <p>Implementations live in separate classes that import provider-specific Spring AI types.
 * The JVM only loads an implementation when it's actually instantiated, so missing provider
 * JARs (e.g. {@code spring-ai-anthropic}) won't cause {@link NoClassDefFoundError} at
 * startup.</p>
 */
interface ProviderChatOptionsFactory {

    /**
     * Builds provider-specific chat options based on the resolved agent configuration values.
     *
     * @param model          the model identifier (may be {@code null})
     * @param cacheStrategy  the Anthropic cache strategy name (may be {@code null})
     * @param promptCacheKey the OpenAI prompt cache key (may be {@code null})
     * @return a configured {@link ChatOptions.Builder}, or {@code null} if no options needed
     */
    ChatOptions.Builder<?> build(String model, String cacheStrategy, String promptCacheKey);
}

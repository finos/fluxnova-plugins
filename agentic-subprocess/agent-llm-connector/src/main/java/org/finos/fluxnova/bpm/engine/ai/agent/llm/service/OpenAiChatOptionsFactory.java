package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Builds OpenAI-specific chat options including prompt cache key.
 *
 * <p>This class imports {@code spring-ai-openai} types directly. It is only instantiated
 * when {@code spring-ai-openai} is confirmed present on the classpath (checked by
 * {@link SpringAiLlmService} before creating an instance). If the JAR is absent, this class
 * is never loaded by the JVM.</p>
 */
final class OpenAiChatOptionsFactory implements ProviderChatOptionsFactory {

    @Override
    public ChatOptions.Builder<?> build(String model, String cacheStrategy, String promptCacheKey) {
        if (model == null && promptCacheKey == null) {
            return null;
        }
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (model != null) {
            builder.model(model);
        }
        if (promptCacheKey != null) {
            builder.promptCacheKey(promptCacheKey);
        }
        return builder;
    }
}

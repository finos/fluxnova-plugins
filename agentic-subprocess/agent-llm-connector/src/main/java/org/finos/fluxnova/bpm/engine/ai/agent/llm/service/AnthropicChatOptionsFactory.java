package org.finos.fluxnova.bpm.engine.ai.agent.llm.service;

import org.springframework.ai.anthropic.AnthropicCacheOptions;
import org.springframework.ai.anthropic.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.Locale;

/**
 * Builds Anthropic-specific chat options including prompt cache strategy.
 *
 * <p>This class imports {@code spring-ai-anthropic} types directly. It is only instantiated
 * when {@code spring-ai-anthropic} is confirmed present on the classpath (checked by
 * {@link SpringAiLlmService} before creating an instance). If the JAR is absent, this class
 * is never loaded by the JVM.</p>
 */
final class AnthropicChatOptionsFactory implements ProviderChatOptionsFactory {

    @Override
    public ChatOptions.Builder<?> build(String model, String cacheStrategy, String promptCacheKey) {
        if (model == null && cacheStrategy == null) {
            return null;
        }
        AnthropicChatOptions.Builder builder = AnthropicChatOptions.builder();
        if (model != null) {
            builder.model(model);
        }
        if (cacheStrategy != null) {
            AnthropicCacheStrategy strategy = parseCacheStrategy(cacheStrategy);
            AnthropicCacheOptions.Builder cacheOptions =
                    AnthropicCacheOptions.builder().strategy(strategy);
            if (strategy == AnthropicCacheStrategy.CONVERSATION_HISTORY) {
                cacheOptions.cacheToolResults(true);
            }
            builder.cacheOptions(cacheOptions.build());
        }
        return builder;
    }

    private static AnthropicCacheStrategy parseCacheStrategy(String raw) {
        try {
            return AnthropicCacheStrategy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Unknown Anthropic cacheStrategy '"
                            + raw
                            + "'. Expected one of: NONE, TOOLS_ONLY, SYSTEM_ONLY, "
                            + "SYSTEM_AND_TOOLS, CONVERSATION_HISTORY",
                    ex);
        }
    }
}

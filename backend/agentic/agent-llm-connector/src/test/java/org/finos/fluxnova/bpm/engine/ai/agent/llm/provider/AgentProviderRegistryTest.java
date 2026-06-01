package org.finos.fluxnova.bpm.engine.ai.agent.llm.provider;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AgentProviderRegistryTest {

    @Test
    void get_whenProviderIsRegistered_returnsAssociatedChatModel() {
        ChatModel ollama = mock(ChatModel.class);
        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", ollama));

        assertThat(registry.get("ollama")).isSameAs(ollama);
        assertThat(registry.has("ollama")).isTrue();
    }

    @Test
    void get_whenProviderNotRegistered_throwsIllegalStateExceptionWithOverrideHint() {
        AgentProviderRegistry registry = new AgentProviderRegistry(Map::of);

        assertThatThrownBy(() -> registry.get("huggingface"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("huggingface")
            .hasMessageContaining("provider-overrides");
        assertThat(registry.has("huggingface")).isFalse();
    }

    @Test
    void has_whenSupplierInitiallyReturnsEmpty_doesNotCacheAndRetriesOnNextCall() {
        ChatModel ollama = mock(ChatModel.class);
        // Simulates the pre-deploy / early-context scenario: first call returns
        // empty (ChatModel beans not yet initialised), second call returns the
        // real providers once the context is fully ready.
        int[] callCount = {0};
        AgentProviderRegistry registry = new AgentProviderRegistry(() -> {
            callCount[0]++;
            return callCount[0] == 1 ? Map.of() : Map.of("ollama", ollama);
        });

        assertThat(registry.has("ollama")).as("should return false on first (empty) resolution").isFalse();
        assertThat(callCount[0]).isEqualTo(1);
        assertThat(registry.has("ollama")).as("should return true once providers are available").isTrue();
        assertThat(callCount[0]).as("supplier should have been called again after empty result").isEqualTo(2);
        registry.has("ollama");
        assertThat(callCount[0]).as("supplier should not be called again once result is cached").isEqualTo(2);
    }

    @Test
    void get_whenCalledRepeatedly_invokesSupplierOnlyOnFirstCallAndCachesResult() {
        ChatModel ollama = mock(ChatModel.class);
        int[] callCount = {0};
        AgentProviderRegistry registry = new AgentProviderRegistry(() -> {
            callCount[0]++;
            return Map.of("ollama", ollama);
        });

        assertThat(callCount[0]).as("supplier should not be called at construction").isEqualTo(0);
        registry.get("ollama");
        assertThat(callCount[0]).as("supplier should be called on first access").isEqualTo(1);
        registry.get("ollama");
        assertThat(callCount[0]).as("supplier should only be called once").isEqualTo(1);
    }
}

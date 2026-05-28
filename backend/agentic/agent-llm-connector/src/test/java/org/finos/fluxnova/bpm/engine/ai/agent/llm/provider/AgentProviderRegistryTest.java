package org.finos.fluxnova.bpm.engine.ai.agent.llm.provider;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentProviderRegistryTest {

    @Test
    void resolvesRegisteredProvider() {
        ChatModel ollama = mock(ChatModel.class);
        AgentProviderRegistry registry = new AgentProviderRegistry(() -> Map.of("ollama", ollama));

        assertSame(ollama, registry.get("ollama"));
        assertTrue(registry.has("ollama"));
    }

    @Test
    void throwsWhenProviderMissing() {
        AgentProviderRegistry registry = new AgentProviderRegistry(Map::of);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> registry.get("huggingface"));
        assertTrue(ex.getMessage().contains("huggingface"));
        assertTrue(ex.getMessage().contains("provider-overrides"),
            "error should hint at the override mechanism");
        assertFalse(registry.has("huggingface"));
    }

    @Test
    void emptyResultIsNotCachedAllowingRetryOnNextAccess() {
        ChatModel ollama = mock(ChatModel.class);
        // Simulates the pre-deploy / early-context scenario: first call returns
        // empty (ChatModel beans not yet initialised), second call returns the
        // real providers once the context is fully ready.
        int[] callCount = {0};
        AgentProviderRegistry registry = new AgentProviderRegistry(() -> {
            callCount[0]++;
            return callCount[0] == 1 ? Map.of() : Map.of("ollama", ollama);
        });

        assertFalse(registry.has("ollama"), "should return false on first (empty) resolution");
        assertEquals(1, callCount[0]);
        assertTrue(registry.has("ollama"), "should return true once providers are available");
        assertEquals(2, callCount[0], "supplier should have been called again after empty result");
        registry.has("ollama");
        assertEquals(2, callCount[0], "supplier should not be called again once result is cached");
    }

    @Test
    void supplierIsCalledLazilyOnFirstAccess() {
        ChatModel ollama = mock(ChatModel.class);
        int[] callCount = {0};
        AgentProviderRegistry registry = new AgentProviderRegistry(() -> {
            callCount[0]++;
            return Map.of("ollama", ollama);
        });

        assertEquals(0, callCount[0], "supplier should not be called at construction");
        registry.get("ollama");
        assertEquals(1, callCount[0], "supplier should be called on first access");
        registry.get("ollama");
        assertEquals(1, callCount[0], "supplier should only be called once");
    }
}

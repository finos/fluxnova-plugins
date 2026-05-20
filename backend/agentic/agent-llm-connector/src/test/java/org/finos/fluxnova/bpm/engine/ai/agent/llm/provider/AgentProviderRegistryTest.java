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

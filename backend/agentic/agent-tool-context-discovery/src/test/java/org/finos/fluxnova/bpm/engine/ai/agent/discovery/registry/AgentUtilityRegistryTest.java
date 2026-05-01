package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentUtilityBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentUtilityRegistryTest {

    private static final String PROC_DEF_ID = "creditCheck:1:abc";
    private static final String ELEMENT_ID = "creditCheckAgent";
    private static final String SENTINEL = "resolved-value";

    private static final String BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
              <process id="creditCheck">
                <adHocSubProcess id="creditCheckAgent"/>
              </process>
            </definitions>
            """;

    /** Minimal concrete subclass — adds no behaviour of its own. */
    private static class TestRegistry extends AgentUtilityRegistry<String> {
        TestRegistry(RepositoryService repositoryService,
                     AgentConfigRegistry agentConfigRegistry,
                     AgentUtilityBuilder<String> builder) {
            super(repositoryService, agentConfigRegistry, builder);
        }
    }

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private AgentConfigRegistry agentConfigRegistry;

    @Mock
    private AgentUtilityBuilder<String> builder;

    private TestRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TestRegistry(repositoryService, agentConfigRegistry, builder);
    }

    private AgentConfig config() {
        return new AgentConfig(PROC_DEF_ID, ELEMENT_ID, "anthropic", "claude-sonnet-4-6",
                "You are a credit analyst.", ELEMENT_ID);
    }

    private ByteArrayInputStream bpmnStream() {
        return new ByteArrayInputStream(BPMN.getBytes(StandardCharsets.UTF_8));
    }

    // --- no agent config ---

    @Test
    void resolve_whenNoAgentConfig_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());

        Optional<String> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repositoryService);
    }

    // --- happy path ---

    @Test
    void resolve_whenAgentConfigExists_returnsBuilderResult() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        when(builder.build(any(Element.class), eq(PROC_DEF_ID))).thenReturn(SENTINEL);

        Optional<String> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isPresent());
        assertEquals(SENTINEL, result.get());
    }

    // --- caching ---

    @Test
    void resolve_calledTwice_scansOnlyOnce() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        when(builder.build(any(Element.class), eq(PROC_DEF_ID))).thenReturn(SENTINEL);

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(1)).getProcessModel(PROC_DEF_ID);
    }

    // --- unregisterAll ---

    @Test
    void unregisterAll_clearsCache() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        when(builder.build(any(Element.class), eq(PROC_DEF_ID))).thenReturn(SENTINEL);

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.unregisterAll();

        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());
        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
    }

    @Test
    void resolve_afterUnregisterAll_rescans() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        when(builder.build(any(Element.class), eq(PROC_DEF_ID))).thenReturn(SENTINEL);

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.unregisterAll();

        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(2)).getProcessModel(PROC_DEF_ID);
    }

    // --- element lookup ---

    @Test
    void resolve_whenToolScopeElementNotFound_returnsEmpty() {
        AgentConfig configWithMissingScope = new AgentConfig(PROC_DEF_ID, ELEMENT_ID,
                "anthropic", "claude-sonnet-4-6", "prompt", "nonExistentScope");
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(configWithMissingScope));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());

        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
        verifyNoInteractions(builder);
    }

    // --- null / missing process model ---

    @Test
    void resolve_whenGetProcessModelReturnsNull_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(null);

        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
    }

    // --- exception handling ---

    @Test
    void resolve_whenBuilderThrowsProcessEngineException_propagates() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        when(builder.build(any(Element.class), eq(PROC_DEF_ID)))
                .thenThrow(new ProcessEngineException("invalid config"));

        assertThrows(ProcessEngineException.class,
                () -> registry.resolve(PROC_DEF_ID, ELEMENT_ID));
    }

    @Test
    void resolve_whenIOExceptionOnStreamClose_isNotCached_retriesNextCall() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        ByteArrayInputStream throwingStream = new ByteArrayInputStream(
                BPMN.getBytes(StandardCharsets.UTF_8)) {
            private boolean firstCloseDone = false;
            @Override
            public void close() throws IOException {
                if (!firstCloseDone) {
                    firstCloseDone = true;
                    return;
                }
                throw new IOException("close failed");
            }
        };
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(throwingStream);
        when(builder.build(any(Element.class), eq(PROC_DEF_ID))).thenReturn(SENTINEL);

        // IOException is caught → null returned → not cached
        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());

        // Second call retries and succeeds
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isPresent());

        verify(repositoryService, times(2)).getProcessModel(PROC_DEF_ID);
    }
}

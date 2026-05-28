package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.exception.NotFoundException;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentContextSpecRegistryTest {

    private static final String PROC_DEF_ID = "creditCheck:1:abc";
    private static final String ELEMENT_ID = "creditCheckAgent";
    private static final AgentContextSpec SENTINEL =
            new AgentContextSpec(PROC_DEF_ID, ELEMENT_ID, List.of());

    private static final String BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
              <process id="creditCheck">
                <adHocSubProcess id="creditCheckAgent"/>
              </process>
            </definitions>
            """;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private ObjectProvider<RepositoryService> repositoryServiceProvider;

    @Mock
    private AgentConfigRegistry agentConfigRegistry;

    @Mock
    private AgentContextSpecBuilder builder;

    private AgentContextSpecRegistry registry;

    @BeforeEach
    void setUp() {
        lenient().when(repositoryServiceProvider.getObject()).thenReturn(repositoryService);
        registry = new AgentContextSpecRegistry(repositoryServiceProvider, agentConfigRegistry, builder);
    }

    private AgentConfig config() {
        return new AgentConfig(PROC_DEF_ID, ELEMENT_ID, "anthropic", "claude-sonnet-4-6",
                "You are a credit analyst.", ELEMENT_ID);
    }

    private ByteArrayInputStream bpmnStream() {
        return new ByteArrayInputStream(BPMN.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void resolve_whenNoAgentConfig_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());

        Optional<AgentContextSpec> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repositoryService);
    }

    @Test
    void resolve_whenAgentConfigExists_returnsBuilderResult() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        when(builder.build(any(Element.class), eq(PROC_DEF_ID))).thenReturn(SENTINEL);

        Optional<AgentContextSpec> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isPresent());
        assertEquals(SENTINEL, result.get());
    }

    @Test
    void resolve_calledTwice_scansOnlyOnce() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        when(builder.build(any(Element.class), eq(PROC_DEF_ID))).thenReturn(SENTINEL);

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(1)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void resolve_unregisterAll_clearsCache() {
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

    @Test
    void resolve_whenToolScopeElementNotFound_returnsEmpty() {
        AgentConfig configWithMissingScope = new AgentConfig(PROC_DEF_ID, ELEMENT_ID,
                "anthropic", "claude-sonnet-4-6", "prompt", "nonExistentScope");
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(configWithMissingScope));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());

        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
        verifyNoInteractions(builder);
    }

    @Test
    void resolve_whenGetProcessModelReturnsNull_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(null);

        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
    }

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

        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());

        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());
        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isPresent());

        verify(repositoryService, times(2)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void resolve_whenNoAgentConfig_isCached_doesNotRescanOnSecondCall() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(agentConfigRegistry, times(1)).resolve(PROC_DEF_ID, ELEMENT_ID);
    }

    @Test
    void resolve_whenGetProcessModelReturnsNull_isCached_doesNotRescanOnSecondCall() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(null);

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(1)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void resolve_whenToolScopeElementNotFound_isCached_doesNotRescanOnSecondCall() {
        AgentConfig configWithMissingScope = new AgentConfig(PROC_DEF_ID, ELEMENT_ID,
                "anthropic", "claude-sonnet-4-6", "prompt", "nonExistentScope");
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(configWithMissingScope));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(bpmnStream());

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(1)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void resolve_whenNotFoundException_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenThrow(new NotFoundException("not found"));

        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
    }

    @Test
    void resolve_whenAuthorizationException_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenThrow(new AuthorizationException("denied"));

        assertTrue(registry.resolve(PROC_DEF_ID, ELEMENT_ID).isEmpty());
    }
}

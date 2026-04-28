package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentToolCatalogueRegistryTest {

    private static final String PROC_DEF_ID = "creditCheck:1:abc";
    private static final String ELEMENT_ID = "creditCheckAgent";

    private static final String BPMN_WITH_TOOL = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
              <process id="creditCheck">
                <adHocSubProcess id="creditCheckAgent">
                  <extensionElements>
                    <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                  systemPrompt="You are a credit analyst."/>
                  </extensionElements>
                  <serviceTask id="taskA" name="Task A"/>
                </adHocSubProcess>
              </process>
            </definitions>
            """;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private AgentConfigRegistry agentConfigRegistry;

    @Mock
    private AgentToolCatalogueBuilder catalogueBuilder;

    private AgentToolCatalogueRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentToolCatalogueRegistry(repositoryService, agentConfigRegistry, catalogueBuilder);
    }

    private AgentConfig config() {
        return new AgentConfig(PROC_DEF_ID, ELEMENT_ID, "anthropic", "claude-sonnet-4-6",
                "You are a credit analyst.", ELEMENT_ID);
    }

    @Test
    void resolve_whenNoAgentConfig_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());

        Optional<AgentToolCatalogue> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repositoryService);
    }

    @Test
    void resolve_whenAgentConfigExists_buildsCatalogue() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)));
        AgentToolCatalogue expected = new AgentToolCatalogue(PROC_DEF_ID, ELEMENT_ID,
                List.of(new AgentToolEntry("taskA", "Task A", null, Set.of(), Set.of())));
        when(catalogueBuilder.build(any(Element.class), eq(PROC_DEF_ID))).thenReturn(expected);

        Optional<AgentToolCatalogue> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().tools().size());
    }

    @Test
    void resolve_calledTwice_scansOnlyOnce() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)));
        when(catalogueBuilder.build(any(Element.class), eq(PROC_DEF_ID)))
                .thenReturn(new AgentToolCatalogue(PROC_DEF_ID, ELEMENT_ID, List.of()));

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(1)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void unregisterAll_clearsAllState() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)));
        when(catalogueBuilder.build(any(Element.class), eq(PROC_DEF_ID)))
                .thenReturn(new AgentToolCatalogue(PROC_DEF_ID, ELEMENT_ID,
                        List.of(new AgentToolEntry("taskA", "Task A", null, Set.of(), Set.of()))));

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.unregisterAll();

        // After unregister, resolve with no config returns empty
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());
        Optional<AgentToolCatalogue> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_afterUnregisterAll_rescans() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)));
        when(catalogueBuilder.build(any(Element.class), eq(PROC_DEF_ID)))
                .thenReturn(new AgentToolCatalogue(PROC_DEF_ID, ELEMENT_ID, List.of()));

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.unregisterAll();

        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)));

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(2)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void resolve_whenToolScopeElementNotFound_returnsEmpty() {
        AgentConfig configWithDifferentScope = new AgentConfig(PROC_DEF_ID, ELEMENT_ID,
                "anthropic", "claude-sonnet-4-6", "prompt", "nonExistentScope");
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(configWithDifferentScope));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)));

        Optional<AgentToolCatalogue> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_whenToolScopeElementIsNotAdHocSubProcess_returnsEmpty() {
        String bpmnWithRegularSubProcess = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="creditCheck">
                    <subProcess id="creditCheckAgent">
                      <extensionElements>
                        <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                      systemPrompt="You are a credit analyst."/>
                      </extensionElements>
                      <serviceTask id="taskA" name="Task A"/>
                    </subProcess>
                  </process>
                </definitions>
                """;
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(bpmnWithRegularSubProcess.getBytes(StandardCharsets.UTF_8)));

        Optional<AgentToolCatalogue> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(catalogueBuilder);
    }

    @Test
    void resolve_whenBuilderThrowsProcessEngineException_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)));
        when(catalogueBuilder.build(any(Element.class), eq(PROC_DEF_ID)))
                .thenThrow(new ProcessEngineException("invalid config"));

        Optional<AgentToolCatalogue> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_whenIOExceptionOnStreamClose_retriesOnNextCall() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        ByteArrayInputStream stream = new ByteArrayInputStream(
                BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)) {
            private boolean firstCloseDone = false;
            @Override
            public void close() throws IOException {
                if (firstCloseDone) {
                    throw new IOException("close failed");
                }
                firstCloseDone = true;
            }
        };
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(stream);
        when(catalogueBuilder.build(any(Element.class), eq(PROC_DEF_ID)))
                .thenReturn(new AgentToolCatalogue(PROC_DEF_ID, ELEMENT_ID, List.of()));

        // First call - IOException on try-with-resources close after parsing succeeds
        Optional<AgentToolCatalogue> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        // Catalogue was stored before close, so result is present
        assertTrue(result.isPresent());

        // Second call triggers re-scan because doScan returned null
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_TOOL.getBytes(StandardCharsets.UTF_8)));
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(2)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void resolve_whenGetProcessModelReturnsNull_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID)).thenReturn(null);

        Optional<AgentToolCatalogue> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isEmpty());
    }
}

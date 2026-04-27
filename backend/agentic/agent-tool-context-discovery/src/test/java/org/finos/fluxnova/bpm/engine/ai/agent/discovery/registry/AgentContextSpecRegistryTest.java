package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentContextSpecRegistryTest {

    private static final String PROC_DEF_ID = "creditCheck:1:abc";
    private static final String ELEMENT_ID = "creditCheckAgent";

    private static final String BPMN_WITH_CONTEXT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
              <process id="creditCheck">
                <adHocSubProcess id="creditCheckAgent">
                  <extensionElements>
                    <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                  systemPrompt="You are a credit analyst."/>
                    <agent:context>
                      <agent:variable name="customerId"/>
                      <agent:variable name="applicationAmount"/>
                    </agent:context>
                  </extensionElements>
                </adHocSubProcess>
              </process>
            </definitions>
            """;

    private static final String BPMN_WITHOUT_CONTEXT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
              <process id="creditCheck">
                <adHocSubProcess id="creditCheckAgent">
                  <extensionElements>
                    <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                  systemPrompt="You are a credit analyst."/>
                  </extensionElements>
                </adHocSubProcess>
              </process>
            </definitions>
            """;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private AgentConfigRegistry agentConfigRegistry;

    private AgentContextSpecRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentContextSpecRegistry(repositoryService, agentConfigRegistry,
                new AgentContextSpecExtractor());
    }

    private AgentConfig config() {
        return new AgentConfig(PROC_DEF_ID, ELEMENT_ID, "anthropic", "claude-sonnet-4-6",
                "You are a credit analyst.", ELEMENT_ID);
    }

    @Test
    void resolve_whenNoAgentConfig_returnsEmpty() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());

        Optional<AgentContextSpec> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repositoryService);
    }

    @Test
    void resolve_whenAgentContextDeclared_returnsDeclaredVariables() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_CONTEXT.getBytes(StandardCharsets.UTF_8)));

        Optional<AgentContextSpec> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().declaredVariables().size());
        List<String> names = result.get().declaredVariables().stream()
                .map(ContextVariableDeclaration::name).toList();
        assertTrue(names.contains("customerId"));
        assertTrue(names.contains("applicationAmount"));
    }

    @Test
    void resolve_whenNoAgentContext_returnsEmptyDeclaredVariables() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITHOUT_CONTEXT.getBytes(StandardCharsets.UTF_8)));

        Optional<AgentContextSpec> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        assertTrue(result.isPresent());
        assertTrue(result.get().declaredVariables().isEmpty());
    }

    @Test
    void resolve_calledTwice_scansOnlyOnce() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_CONTEXT.getBytes(StandardCharsets.UTF_8)));

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(1)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void unregisterAll_clearsAllState() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_CONTEXT.getBytes(StandardCharsets.UTF_8)));

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.unregisterAll();

        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.empty());
        Optional<AgentContextSpec> result = registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_afterUnregisterAll_rescans() {
        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_CONTEXT.getBytes(StandardCharsets.UTF_8)));

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);
        registry.unregisterAll();

        when(agentConfigRegistry.resolve(PROC_DEF_ID, ELEMENT_ID)).thenReturn(Optional.of(config()));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_CONTEXT.getBytes(StandardCharsets.UTF_8)));

        registry.resolve(PROC_DEF_ID, ELEMENT_ID);

        verify(repositoryService, times(2)).getProcessModel(PROC_DEF_ID);
    }

    @Test
    void resolve_whenElementNotFoundInBpmn_returnsEmpty() {
        AgentConfig configWithWrongId = new AgentConfig(PROC_DEF_ID, "nonExistentElement",
                "anthropic", "claude-sonnet-4-6", "prompt", ELEMENT_ID);
        when(agentConfigRegistry.resolve(PROC_DEF_ID, "nonExistentElement"))
                .thenReturn(Optional.of(configWithWrongId));
        when(repositoryService.getProcessModel(PROC_DEF_ID))
                .thenReturn(new ByteArrayInputStream(BPMN_WITH_CONTEXT.getBytes(StandardCharsets.UTF_8)));

        Optional<AgentContextSpec> result = registry.resolve(PROC_DEF_ID, "nonExistentElement");

        assertTrue(result.isEmpty());
    }
}

package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorEnginePluginTest {

    @Mock
    private AdHocAgentOrchestrationParseListener parseListener;
    @Mock
    private ProcessEngineConfigurationImpl processEngineConfiguration;

    @Nested
    class PreInit {

        @Captor
        ArgumentCaptor<List<BpmnParseListener>> captor;

        @Test
        void preInit_withExistingListeners_appendsParseListener() {
            BpmnParseListener existingListener = mock(BpmnParseListener.class);
            when(processEngineConfiguration.getCustomPostBPMNParseListeners())
                    .thenReturn(new ArrayList<>(List.of(existingListener)));

            AgentOrchestratorEnginePlugin plugin = new AgentOrchestratorEnginePlugin(parseListener);
            plugin.preInit(processEngineConfiguration);

            verify(processEngineConfiguration).setCustomPostBPMNParseListeners(captor.capture());
            List<BpmnParseListener> result = captor.getValue();

            assertEquals(2, result.size());
            assertSame(existingListener, result.get(0));
            assertSame(parseListener, result.get(1));
        }

        @Test
        void preInit_whenListIsNull_createsNewList() {
            when(processEngineConfiguration.getCustomPostBPMNParseListeners()).thenReturn(null);

            AgentOrchestratorEnginePlugin plugin = new AgentOrchestratorEnginePlugin(parseListener);
            plugin.preInit(processEngineConfiguration);

            verify(processEngineConfiguration).setCustomPostBPMNParseListeners(captor.capture());
            List<BpmnParseListener> result = captor.getValue();

            assertEquals(1, result.size());
            assertSame(parseListener, result.get(0));
        }
    }
}

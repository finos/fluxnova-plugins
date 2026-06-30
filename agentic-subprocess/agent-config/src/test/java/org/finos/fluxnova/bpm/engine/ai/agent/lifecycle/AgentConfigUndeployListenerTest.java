package org.finos.fluxnova.bpm.engine.ai.agent.lifecycle;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.spring.boot.starter.event.PreUndeployEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentConfigUndeployListenerTest {

    @Mock
    private AgentConfigRegistry registry;

    @InjectMocks
    private AgentConfigUndeployListener listener;

    @Test
    void onPreUndeploy_callsUnregisterAll() {
        PreUndeployEvent event = new PreUndeployEvent(mock(ProcessEngine.class));

        listener.onPreUndeploy(event);

        verify(registry).unregisterAll();
    }
}

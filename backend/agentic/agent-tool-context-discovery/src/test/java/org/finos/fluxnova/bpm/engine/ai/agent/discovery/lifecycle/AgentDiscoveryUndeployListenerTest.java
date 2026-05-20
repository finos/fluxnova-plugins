package org.finos.fluxnova.bpm.engine.ai.agent.discovery.lifecycle;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.spring.boot.starter.event.PreUndeployEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentDiscoveryUndeployListenerTest {

    @Mock
    private AgentToolCatalogueRegistry catalogueRegistry;

    @Mock
    private AgentContextSpecRegistry contextSpecRegistry;

    @InjectMocks
    private AgentDiscoveryUndeployListener listener;

    @Test
    void onPreUndeploy_clearsBothRegistries() {
        PreUndeployEvent event = new PreUndeployEvent(mock(ProcessEngine.class));

        listener.onPreUndeploy(event);

        verify(catalogueRegistry).unregisterAll();
        verify(contextSpecRegistry).unregisterAll();
    }
}

package org.finos.fluxnova.bpm.engine.ai.agent.llm.parser;

import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentProviderEnginePluginTest {

    @Test
    void preInit_registersParseListenerInCustomPostBPMNParseListeners() {
        AgentProviderRegistry registry = mock(AgentProviderRegistry.class);
        AgentProviderEnginePlugin plugin = new AgentProviderEnginePlugin(registry);
        ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
        when(config.getCustomPostBPMNParseListeners()).thenReturn(null);

        plugin.preInit(config);

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(config).setCustomPostBPMNParseListeners(captor.capture());
        List<BpmnParseListener> listeners = captor.getValue();
        assertThat(listeners).hasSize(1);
        assertThat(listeners.get(0)).isInstanceOf(AgentProviderParseListener.class);
    }

    @Test
    void preInit_appendsToExistingListeners() {
        AgentProviderRegistry registry = mock(AgentProviderRegistry.class);
        AgentProviderEnginePlugin plugin = new AgentProviderEnginePlugin(registry);
        ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);

        BpmnParseListener existing = mock(BpmnParseListener.class);
        java.util.ArrayList<BpmnParseListener> existingList = new java.util.ArrayList<>();
        existingList.add(existing);
        when(config.getCustomPostBPMNParseListeners()).thenReturn(existingList);

        plugin.preInit(config);

        assertThat(existingList).hasSize(2);
        assertThat(existingList.get(1)).isInstanceOf(AgentProviderParseListener.class);
    }
}

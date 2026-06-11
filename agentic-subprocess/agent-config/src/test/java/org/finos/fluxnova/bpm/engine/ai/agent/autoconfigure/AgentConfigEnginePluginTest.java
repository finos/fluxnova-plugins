package org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure;

import org.finos.fluxnova.bpm.engine.ai.agent.parser.AgentConfigParseListener;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigEnginePluginTest {

    private final AgentConfigEnginePlugin plugin = new AgentConfigEnginePlugin();

    @Test
    void preInit_whenListenerListNull_initialisesAndRegistersListener() {
        ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
        config.setCustomPostBPMNParseListeners(null);

        plugin.preInit(config);

        List<BpmnParseListener> listeners = config.getCustomPostBPMNParseListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertTrue(listeners.get(0) instanceof AgentConfigParseListener);
    }

    @Test
    void preInit_whenListenerListExists_appendsListenerWithoutClobbering() {
        ProcessEngineConfigurationImpl config = new StandaloneInMemProcessEngineConfiguration();
        BpmnParseListener existing = new AgentConfigParseListener();
        List<BpmnParseListener> existingList = new ArrayList<>();
        existingList.add(existing);
        config.setCustomPostBPMNParseListeners(existingList);

        plugin.preInit(config);

        List<BpmnParseListener> listeners = config.getCustomPostBPMNParseListeners();
        assertEquals(2, listeners.size());
        assertTrue(listeners.get(0) == existing);
        assertTrue(listeners.get(1) instanceof AgentConfigParseListener);
    }
}

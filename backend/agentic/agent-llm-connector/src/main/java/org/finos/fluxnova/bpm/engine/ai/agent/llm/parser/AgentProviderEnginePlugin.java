package org.finos.fluxnova.bpm.engine.ai.agent.llm.parser;

import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

import java.util.ArrayList;
import java.util.List;

public class AgentProviderEnginePlugin extends AbstractProcessEnginePlugin {

    private final AgentProviderRegistry registry;

    public AgentProviderEnginePlugin(AgentProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<BpmnParseListener> listeners = processEngineConfiguration.getCustomPostBPMNParseListeners();
        if (listeners == null) {
            listeners = new ArrayList<>();
            processEngineConfiguration.setCustomPostBPMNParseListeners(listeners);
        }
        listeners.add(new AgentProviderParseListener(registry));
    }
}

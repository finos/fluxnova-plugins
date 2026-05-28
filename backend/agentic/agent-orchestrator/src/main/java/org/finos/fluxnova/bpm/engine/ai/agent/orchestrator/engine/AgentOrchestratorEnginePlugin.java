package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine;

import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AgentOrchestratorEnginePlugin extends AbstractProcessEnginePlugin {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestratorEnginePlugin.class);
    private final BpmnParseListener parseListener;

    public AgentOrchestratorEnginePlugin(BpmnParseListener parseListener) {
        this.parseListener = parseListener;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<BpmnParseListener> existing =
                processEngineConfiguration.getCustomPostBPMNParseListeners();
        List<BpmnParseListener> listeners =
                new ArrayList<>(existing != null ? existing : List.of());
        listeners.add(parseListener);
        processEngineConfiguration.setCustomPostBPMNParseListeners(listeners);
        log.info("Registered Agent Orchestrator BPMN parse listener: {}", parseListener.getClass().getSimpleName());
    }
}

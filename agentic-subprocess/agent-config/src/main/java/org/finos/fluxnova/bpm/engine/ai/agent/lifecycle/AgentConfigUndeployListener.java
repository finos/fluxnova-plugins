package org.finos.fluxnova.bpm.engine.ai.agent.lifecycle;

import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.spring.boot.starter.event.PreUndeployEvent;
import org.springframework.context.event.EventListener;

public class AgentConfigUndeployListener {

    private final AgentConfigRegistry registry;

    public AgentConfigUndeployListener(AgentConfigRegistry registry) {
        this.registry = registry;
    }

    @EventListener
    public void onPreUndeploy(PreUndeployEvent event) {
        registry.unregisterAll();
    }
}

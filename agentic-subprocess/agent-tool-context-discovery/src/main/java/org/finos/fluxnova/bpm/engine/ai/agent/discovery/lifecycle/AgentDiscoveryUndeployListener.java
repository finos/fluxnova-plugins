package org.finos.fluxnova.bpm.engine.ai.agent.discovery.lifecycle;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.spring.boot.starter.event.PreUndeployEvent;
import org.springframework.context.event.EventListener;

public class AgentDiscoveryUndeployListener {

    private final AgentToolCatalogueRegistry catalogueRegistry;
    private final AgentContextSpecRegistry contextSpecRegistry;

    public AgentDiscoveryUndeployListener(AgentToolCatalogueRegistry catalogueRegistry,
                                          AgentContextSpecRegistry contextSpecRegistry) {
        this.catalogueRegistry = catalogueRegistry;
        this.contextSpecRegistry = contextSpecRegistry;
    }

    @EventListener
    public void onPreUndeploy(PreUndeployEvent event) {
        catalogueRegistry.unregisterAll();
        contextSpecRegistry.unregisterAll();
    }
}

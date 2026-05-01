package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;

public class AgentToolCatalogueRegistry extends AgentUtilityRegistry<AgentToolCatalogue> {

    public AgentToolCatalogueRegistry(RepositoryService repositoryService,
                                      AgentConfigRegistry agentConfigRegistry,
                                      AgentToolCatalogueBuilder catalogueBuilder) {
        super(repositoryService, agentConfigRegistry, catalogueBuilder);
    }
}

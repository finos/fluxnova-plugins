package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;

public class AgentContextSpecRegistry extends AgentUtilityRegistry<AgentContextSpec> {

    public AgentContextSpecRegistry(RepositoryService repositoryService,
                                    AgentConfigRegistry agentConfigRegistry,
                                    AgentContextSpecBuilder extractor) {
        super(repositoryService, agentConfigRegistry, extractor);
    }
}

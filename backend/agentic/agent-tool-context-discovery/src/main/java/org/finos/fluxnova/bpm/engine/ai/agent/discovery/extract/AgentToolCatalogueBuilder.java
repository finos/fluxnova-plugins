package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;

public interface AgentToolCatalogueBuilder {
    AgentToolCatalogue build(ActivityImpl scope);
}

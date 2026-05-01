package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;

public interface AgentToolCatalogueBuilder extends AgentUtilityBuilder<AgentToolCatalogue> {
    AgentToolCatalogue build(Element scopeElement, String processDefinitionId);
}

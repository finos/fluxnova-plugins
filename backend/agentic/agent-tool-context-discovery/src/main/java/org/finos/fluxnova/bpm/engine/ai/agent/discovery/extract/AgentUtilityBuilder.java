package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;

public interface AgentUtilityBuilder<T> {
    T build(Element scopeElement, String processDefinitionId);
}

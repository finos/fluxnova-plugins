package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;

public interface AgentContextSpecExtractor {
    AgentContextSpec extract(Element adHocElement, String processDefinitionId);
}

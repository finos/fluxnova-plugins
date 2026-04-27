package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;

import java.util.ArrayList;
import java.util.List;

public class AgentContextSpecExtractor {

    private static final Namespace AGENT_NS = new Namespace("http://fluxnova.finos.org/schema/1.0/ai/agent");

    public AgentContextSpec extract(Element adHocElement, String processDefinitionId) {
        String elementId = adHocElement.attribute("id");
        Element ext = adHocElement.element("extensionElements");
        if (ext == null) {
            return new AgentContextSpec(processDefinitionId, elementId, List.of());
        }

        Element contextElement = ext.elementNS(AGENT_NS, "context");
        if (contextElement == null) {
            return new AgentContextSpec(processDefinitionId, elementId, List.of());
        }

        List<ContextVariableDeclaration> vars = new ArrayList<>();
        for (Element variable : contextElement.elementsNS(AGENT_NS, "variable")) {
            String name = variable.attribute("name");
            if (name != null && !name.isBlank()) {
                vars.add(new ContextVariableDeclaration(name));
            }
        }

        return new AgentContextSpec(processDefinitionId, elementId, vars);
    }
}

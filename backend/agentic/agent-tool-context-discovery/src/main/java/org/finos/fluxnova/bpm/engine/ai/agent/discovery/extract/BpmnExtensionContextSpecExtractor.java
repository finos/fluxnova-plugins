package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts an {@link AgentContextSpec} from a BPMN element's {@code <extensionElements>},
 * reading declared context variables from an {@code <agent:context>} block.
 */
public class BpmnExtensionContextSpecExtractor implements AgentContextSpecExtractor {

    @Override
    public AgentContextSpec extract(Element element, String processDefinitionId) {
        String elementId = element.attribute("id");
        Element extensionElements = element.element("extensionElements");
        if (extensionElements == null) {
            return new AgentContextSpec(processDefinitionId, elementId, List.of());
        }

        Element contextElement = extensionElements.elementNS(AgentModelConstants.AGENT_NS, "context");
        if (contextElement == null) {
            // Context is empty, so adding empty list here adds all process variables to context
            return new AgentContextSpec(processDefinitionId, elementId, List.of());
        }

        List<ContextVariableDeclaration> variableDeclarations = new ArrayList<>();
        for (Element variable : contextElement.elementsNS(AgentModelConstants.AGENT_NS, "variable")) {
            String name = variable.attribute("name");
            if (name != null && !name.isBlank()) {
                variableDeclarations.add(new ContextVariableDeclaration(name));
            }
        }

        return new AgentContextSpec(processDefinitionId, elementId, variableDeclarations);
    }
}

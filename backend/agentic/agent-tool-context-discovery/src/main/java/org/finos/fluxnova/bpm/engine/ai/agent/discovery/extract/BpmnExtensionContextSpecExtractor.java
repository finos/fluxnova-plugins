package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts an {@link AgentContextSpec} from a BPMN element's {@code <extensionElements>},
 * reading declared context variables from an {@code <agent:context>} block.
 */
public class BpmnExtensionContextSpecExtractor implements AgentContextSpecExtractor {

    private static final Namespace AGENT_NS = new Namespace("http://fluxnova.finos.org/schema/1.0/ai/agent");

    @Override
    public AgentContextSpec extract(Element adHocElement, String processDefinitionId) {
        String elementId = adHocElement.attribute("id");
        Element extensionElements = adHocElement.element("extensionElements");
        if (extensionElements == null) {
            return new AgentContextSpec(processDefinitionId, elementId, List.of());
        }

        Element contextElement = extensionElements.elementNS(AGENT_NS, "context");
        if (contextElement == null) {
            // Context is empty, so adding empty list here adds all process variables to context
            return new AgentContextSpec(processDefinitionId, elementId, List.of());
        }

        List<ContextVariableDeclaration> variableDeclarations = new ArrayList<>();
        for (Element variable : contextElement.elementsNS(AGENT_NS, "variable")) {
            String name = variable.attribute("name");
            if (name != null && !name.isBlank()) {
                variableDeclarations.add(new ContextVariableDeclaration(name));
            }
        }

        return new AgentContextSpec(processDefinitionId, elementId, variableDeclarations);
    }
}

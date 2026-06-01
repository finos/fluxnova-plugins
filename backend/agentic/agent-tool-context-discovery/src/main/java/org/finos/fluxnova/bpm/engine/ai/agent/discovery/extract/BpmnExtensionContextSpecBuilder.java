package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants;

import java.util.List;

/**
 * Extracts an {@link AgentContextSpec} from a BPMN element's {@code <extensionElements>},
 * reading declared context variables from an {@code <agent:context>} block.
 */
public class BpmnExtensionContextSpecBuilder implements AgentContextSpecBuilder {

    @Override
    public AgentContextSpec build(Element element, String processDefinitionId) {
        String elementId = element.attribute("id");
        Element extensionElements = element.element("extensionElements");
        if (extensionElements == null) {
            return new AgentContextSpec(processDefinitionId, elementId, List.of());
        }

        Element contextElement = extensionElements.elementNS(AgentModelConstants.AGENT_NS, "context");
        if (contextElement == null) {
            return new AgentContextSpec(processDefinitionId, elementId, List.of());
        }

        List<ContextVariableDeclaration> variableDeclarations = contextElement
                .elementsNS(AgentModelConstants.AGENT_NS, "variable").stream()
                .map(v -> v.attribute("name"))
                .filter(name -> name != null && !name.isBlank())
                .map(ContextVariableDeclaration::new)
                .toList();

        return new AgentContextSpec(processDefinitionId, elementId, variableDeclarations);
    }
}

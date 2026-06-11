package org.finos.fluxnova.bpm.engine.ai.agent.parser;

import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigElementWalker;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigValidator;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AgentConfigParseListener extends AbstractBpmnParseListener {

    private final AgentConfigElementWalker walker;

    public AgentConfigParseListener() {
        this(new AgentConfigElementWalker());
    }

    AgentConfigParseListener(AgentConfigElementWalker walker) {
        this.walker = walker;
    }

    @Override
    public void parseRootElement(Element rootElement, List<ProcessDefinitionEntity> processDefinitions) {

        for (Element process : rootElement.elements("process")) {
            List<Element> walked = walker.walk(process);
            Set<String> elementIds = collectElementIds(process, walked);

            for (Element element : walked) {
                Element ext = element.element("extensionElements");
                if (ext == null) {
                    continue;
                }
                Element config = ext.elementNS(AgentModelConstants.AGENT_NS, "config");
                if (config == null) {
                    continue;
                }
                List<String> elementErrors = AgentConfigValidator.validate(config, element.attribute("id"), elementIds);
                if (!elementErrors.isEmpty()) {
                    throw new BpmnParseException(String.join("; ", elementErrors), element);
                }
            }
        }
    }

    private static Set<String> collectElementIds(Element process, List<Element> walked) {
        Set<String> ids = new HashSet<>();
        String processId = process.attribute("id");
        if (processId != null) {
            ids.add(processId);
        }
        for (Element element : walked) {
            String id = element.attribute("id");
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}

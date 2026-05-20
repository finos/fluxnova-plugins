package org.finos.fluxnova.bpm.engine.ai.agent.llm.parser;

import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigElementWalker;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants.AGENT_NS;

public class AgentProviderParseListener extends AbstractBpmnParseListener {

    private final AgentProviderRegistry registry;
    private final AgentConfigElementWalker walker;

    public AgentProviderParseListener(AgentProviderRegistry registry) {
        this(registry, new AgentConfigElementWalker());
    }

    AgentProviderParseListener(AgentProviderRegistry registry, AgentConfigElementWalker walker) {
        this.registry = registry;
        this.walker = walker;
    }

    @Override
    public void parseRootElement(Element rootElement, List<ProcessDefinitionEntity> processDefinitions) {
        List<String> errors = new ArrayList<>();
        Element firstOffending = null;

        for (Element process : rootElement.elements("process")) {
            for (Element element : walker.walk(process)) {
                Element ext = element.element("extensionElements");
                if (ext == null) {
                    continue;
                }
                Element config = ext.elementNS(AGENT_NS, "config");
                if (config == null) {
                    continue;
                }
                String provider = config.attribute("provider");
                if (provider == null || provider.isBlank()) {
                    continue;
                }
                if (!registry.has(provider)) {
                    if (firstOffending == null) {
                        firstOffending = element;
                    }
                    String elementId = element.attribute("id");
                    errors.add("element '" + elementId + "' references unavailable provider '" + provider + "'");
                }
            }
        }

        if (!errors.isEmpty()) {
            StringJoiner joiner = new StringJoiner("; ");
            errors.forEach(joiner::add);
            throw new BpmnParseException(
                    "Invalid provider reference(s) in agent:config: " + joiner,
                    firstOffending);
        }
    }
}

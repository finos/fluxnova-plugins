package org.finos.fluxnova.bpm.engine.ai.agent.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants.AGENT_NS;

public class AgentConfigExtractor {

    private final AgentConfigElementWalker walker;

    public AgentConfigExtractor() {
        this(new AgentConfigElementWalker());
    }

    AgentConfigExtractor(AgentConfigElementWalker walker) {
        this.walker = walker;
    }

    public List<AgentConfig> extractAll(InputStream bpmnXml, String processDefinitionId) {
        String processKey = extractProcessKey(processDefinitionId);
        List<AgentConfig> results = new ArrayList<>();
        Parse parse = new BpmnXmlParser().createParse().sourceInputStream(bpmnXml).execute();
        Element root = parse.getRootElement();
        List<Element> processes = root.elements("process");

        // First pass: try matching by extracted process key (key:version:id format)
        for (Element process : processes) {
            if (processKey.equals(process.attribute("id"))) {
                for (Element element : walker.walk(process)) {
                    extract(element, processDefinitionId).ifPresent(results::add);
                }
                return results;
            }
        }

        // Fallback: when process definition ID is a bare UUID (no colon separator),
        // the key extraction returns the full ID which won't match any <process id="...">.
        // In this case, scan the first executable process (or the only process) since
        // getProcessModel() already returns the correct BPMN resource for this definition.
        for (Element process : processes) {
            String isExecutable = process.attribute("isExecutable");
            if (!"false".equals(isExecutable)) {
                for (Element element : walker.walk(process)) {
                    extract(element, processDefinitionId).ifPresent(results::add);
                }
                if (!results.isEmpty()) {
                    return results;
                }
            }
        }

        return results;
    }

    private static String extractProcessKey(String processDefinitionId) {
        int colonIndex = processDefinitionId.indexOf(':');
        return colonIndex >= 0 ? processDefinitionId.substring(0, colonIndex) : processDefinitionId;
    }

    public Optional<AgentConfig> extract(Element element, String processDefinitionId) {
        Element ext = element.element("extensionElements");
        if (ext == null) {
            return Optional.empty();
        }

        Element config = ext.elementNS(AGENT_NS, "config");
        if (config == null) {
            return Optional.empty();
        }

        String elementId = element.attribute("id");
        String provider = config.attribute("provider");
        String model = config.attribute("model");
        String systemPrompt = config.attribute("systemPrompt");

        String toolScopeElementId = config.attribute("toolScopeElementId");
        if (isBlankOrNull(toolScopeElementId)) {
            toolScopeElementId = elementId;
        }

        return Optional.of(new AgentConfig(processDefinitionId, elementId, provider, model, systemPrompt, toolScopeElementId));
    }

    private static boolean isBlankOrNull(String value) {
        return value == null || value.isBlank();
    }
}

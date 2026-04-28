package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AgentContextSpecRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AgentContextSpecRegistry.class);
    private static final String AD_HOC_SUB_PROCESS_TAG = "adHocSubProcess";

    private final ConcurrentHashMap<String, AgentContextSpec> specs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> scanResults = new ConcurrentHashMap<>();

    private final RepositoryService repositoryService;
    private final AgentConfigRegistry agentConfigRegistry;
    private final AgentContextSpecExtractor extractor;

    public AgentContextSpecRegistry(RepositoryService repositoryService,
                                    AgentConfigRegistry agentConfigRegistry,
                                    AgentContextSpecExtractor extractor) {
        this.repositoryService = repositoryService;
        this.agentConfigRegistry = agentConfigRegistry;
        this.extractor = extractor;
    }

    public Optional<AgentContextSpec> resolve(String processDefinitionId, String elementId) {
        ensureScanned(processDefinitionId, elementId);
        return Optional.ofNullable(specs.get(key(processDefinitionId, elementId)));
    }

    public void unregisterAll() {
        specs.clear();
        scanResults.clear();
    }

    private void ensureScanned(String processDefinitionId, String elementId) {
        String cacheKey = key(processDefinitionId, elementId);
        scanResults.computeIfAbsent(cacheKey, ignored -> doScan(processDefinitionId, elementId));
    }

    private Boolean doScan(String processDefinitionId, String elementId) {
        Optional<AgentConfig> config = agentConfigRegistry.resolve(processDefinitionId, elementId);
        if (config.isEmpty()) {
            return Boolean.TRUE;
        }

        try (InputStream xml = repositoryService.getProcessModel(processDefinitionId)) {
            Parse parse = new BpmnXmlParser().createParse().sourceInputStream(xml).execute();
            Element root = parse.getRootElement();

            Element agentSubprocessElement = findElementById(root, elementId);
            if (agentSubprocessElement == null) {
                LOG.warn("Ad-hoc subprocess element '{}' not found in process definition '{}'", elementId, processDefinitionId);
                return Boolean.TRUE;
            }

            if (!AD_HOC_SUB_PROCESS_TAG.equals(agentSubprocessElement.getTagName())) {
                LOG.warn("Element '{}' in process definition '{}' is not an ad-hoc subprocess (found: '{}')",
                        elementId, processDefinitionId, agentSubprocessElement.getTagName());
                return Boolean.TRUE;
            }

            AgentContextSpec spec = extractor.extract(agentSubprocessElement, processDefinitionId);
            specs.put(key(processDefinitionId, elementId), spec);
            
            return Boolean.TRUE; 
        } catch (ProcessEngineException e) {
            LOG.error("Invalid tool configuration in process definition '{}': {}",
                    processDefinitionId, e.getMessage());
            return Boolean.TRUE;
        } catch (IOException e) {
            LOG.error("Failed to scan process definition '{}' for context spec", processDefinitionId, e);
            // if IOException occurs, return null so we can retry next time
            return null;
        }
    }

    private Element findElementById(Element element, String id) {
        if (id.equals(element.attribute("id"))) return element;
        for (Element child : element.elements()) {
            Element found = findElementById(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private static String key(String processDefinitionId, String elementId) {
        return processDefinitionId + "#" + elementId;
    }
}

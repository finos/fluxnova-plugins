package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.exception.NotFoundException;
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

    private final ConcurrentHashMap<String, Optional<AgentContextSpec>> cache = new ConcurrentHashMap<>();

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
        String cacheKey = key(processDefinitionId, elementId);
        Optional<AgentContextSpec> result = cache.computeIfAbsent(cacheKey,
                ignored -> doScan(processDefinitionId, elementId));
        // The null result hasn't been stored, but return an empty optional until the rescan (DISCUSS)
        return result != null ? result : Optional.empty();
    }

    public void unregisterAll() {
        cache.clear();
    }

    private Optional<AgentContextSpec> doScan(String processDefinitionId, String elementId) {
        Optional<AgentConfig> config = agentConfigRegistry.resolve(processDefinitionId, elementId);
        if (config.isEmpty()) {
            return Optional.empty();
        }

        try (InputStream xml = repositoryService.getProcessModel(processDefinitionId)) {
            if (xml == null) {
                LOG.warn("Process model not found for '{}'", processDefinitionId);
                return Optional.empty();
            }

            Parse parse = new BpmnXmlParser().createParse().sourceInputStream(xml).execute();
            Element root = parse.getRootElement();

            Element agentSubprocessElement = findElementById(root, elementId);
            if (agentSubprocessElement == null) {
                LOG.warn("Ad-hoc subprocess element '{}' not found in process definition '{}'", elementId, processDefinitionId);
                return Optional.empty();
            }

            if (!AD_HOC_SUB_PROCESS_TAG.equals(agentSubprocessElement.getTagName())) {
                LOG.warn("Element '{}' in process definition '{}' is not an ad-hoc subprocess (found: '{}')",
                        elementId, processDefinitionId, agentSubprocessElement.getTagName());
                return Optional.empty();
            }

            AgentContextSpec spec = extractor.extract(agentSubprocessElement, processDefinitionId);
            return Optional.of(spec);
        } catch (IOException e) {
            LOG.error("Failed to scan process definition '{}' for context spec", processDefinitionId, e);
            return null; // transient failure — don't cache, retry next time
        } catch (NotFoundException e) {
            LOG.error("Process definition '{}' not found", processDefinitionId, e);
            throw e;
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

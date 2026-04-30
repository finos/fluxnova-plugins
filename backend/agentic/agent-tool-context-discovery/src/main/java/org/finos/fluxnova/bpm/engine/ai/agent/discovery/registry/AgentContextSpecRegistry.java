package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
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
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AgentContextSpecRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AgentContextSpecRegistry.class);

    private final ConcurrentHashMap<String, HashMap<String, AgentContextSpec>> cache = new ConcurrentHashMap<>();

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

    public AgentContextSpec resolve(String processDefinitionId, String elementId) {
        HashMap<String, AgentContextSpec> cachedContextMap 
                = cache.computeIfAbsent(processDefinitionId, keyId -> new HashMap<>());
                
        AgentContextSpec result = cachedContextMap.computeIfAbsent(elementId,
                keyId -> doScan(processDefinitionId, keyId));
        return result;
    }

    public void unregisterAll() {
        cache.clear();
    }

    private AgentContextSpec doScan(String processDefinitionId, String elementId) {
        AgentConfig config = agentConfigRegistry.resolve(processDefinitionId, elementId);
        if (config.isEmpty()) {
            return null;
        }

        try (InputStream xml = repositoryService.getProcessModel(processDefinitionId)) {
            if (xml == null) {
                LOG.warn("Process model not found for '{}'", processDefinitionId);
                return null;
            }

            Parse parse = new BpmnXmlParser().createParse().sourceInputStream(xml).execute();
            Element root = parse.getRootElement();

            Element agentSubprocessElement = findElementById(root, elementId);
            if (agentSubprocessElement == null) {
                LOG.warn("Ad-hoc subprocess element '{}' not found in process definition '{}'", elementId, processDefinitionId);
                return null;
            }

            AgentContextSpec spec = extractor.extract(agentSubprocessElement, processDefinitionId);
            return spec;
        } catch (IOException e) {
            LOG.error("Failed to scan process definition '{}' for context spec", processDefinitionId, e);
            return null; // transient failure — don't cache, retry next time
        } catch (NotFoundException e) {
            LOG.error("Process definition '{}' not found", processDefinitionId, e);
            throw e;
        } catch (AuthorizationException e) {
            LOG.error("Unauthorized process definition access attempt on '{}'", processDefinitionId, e);
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

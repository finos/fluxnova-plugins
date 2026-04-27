package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.BpmnXmlParser;
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

public class AgentToolCatalogueRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AgentToolCatalogueRegistry.class);

    private final ConcurrentHashMap<String, AgentToolCatalogue> catalogues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> scanned = new ConcurrentHashMap<>();

    private final RepositoryService repositoryService;
    private final AgentConfigRegistry agentConfigRegistry;
    private final AgentToolCatalogueBuilder catalogueBuilder;

    public AgentToolCatalogueRegistry(RepositoryService repositoryService,
                                      AgentConfigRegistry agentConfigRegistry,
                                      AgentToolCatalogueBuilder catalogueBuilder) {
        this.repositoryService = repositoryService;
        this.agentConfigRegistry = agentConfigRegistry;
        this.catalogueBuilder = catalogueBuilder;
    }

    public Optional<AgentToolCatalogue> resolve(String processDefinitionId, String elementId) {
        ensureScanned(processDefinitionId, elementId);
        return Optional.ofNullable(catalogues.get(key(processDefinitionId, elementId)));
    }

    public void unregisterAll() {
        catalogues.clear();
        scanned.clear();
    }

    private void ensureScanned(String processDefinitionId, String elementId) {
        String k = key(processDefinitionId, elementId);
        scanned.computeIfAbsent(k, ignored -> doScan(processDefinitionId, elementId));
    }

    private Boolean doScan(String processDefinitionId, String elementId) {
        Optional<AgentConfig> config = agentConfigRegistry.resolve(processDefinitionId, elementId);
        if (config.isEmpty()) {
            return Boolean.TRUE;
        }

        try (InputStream xml = repositoryService.getProcessModel(processDefinitionId)) {
            Parse parse = new BpmnXmlParser().createParse().sourceInputStream(xml).execute();
            Element root = parse.getRootElement();

            String toolScope = config.get().toolScopeElementId();
            Element scopeElement = findElementById(root, toolScope);
            if (scopeElement == null) {
                LOG.warn("Tool scope element '{}' not found in process definition '{}'", toolScope, processDefinitionId);
                return Boolean.TRUE;
            }
            AgentToolCatalogue catalogue = catalogueBuilder.build(scopeElement, processDefinitionId);
            catalogues.put(key(processDefinitionId, elementId), catalogue);
            return Boolean.TRUE;
        } catch (ProcessEngineException e) {
            LOG.error("Invalid tool configuration in process definition '{}': {}",
                    processDefinitionId, e.getMessage());
            return Boolean.TRUE;
        } catch (IOException e) {
            LOG.error("Failed to scan process definition '{}' for tool catalogue", processDefinitionId, e);
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

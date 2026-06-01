package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.exception.NotFoundException;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AgentContextSpecRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AgentContextSpecRegistry.class);

    private final ConcurrentHashMap<String, HashMap<String, AgentContextSpec>> cache = new ConcurrentHashMap<>();

    private final ObjectProvider<RepositoryService> repositoryService;
    private final AgentConfigRegistry agentConfigRegistry;
    private final AgentContextSpecBuilder builder;

    public AgentContextSpecRegistry(ObjectProvider<RepositoryService> repositoryService,
                                    AgentConfigRegistry agentConfigRegistry,
                                    AgentContextSpecBuilder builder) {
        this.repositoryService = repositoryService;
        this.agentConfigRegistry = agentConfigRegistry;
        this.builder = builder;
    }

    public Optional<AgentContextSpec> resolve(String processDefinitionId, String elementId) {
        HashMap<String, AgentContextSpec> resultMap = cache.compute(processDefinitionId, (key, value) -> {
            HashMap<String, AgentContextSpec> currentMap = value != null ? value : new HashMap<>();
            if (!currentMap.containsKey(elementId)) {
                try {
                    AgentContextSpec result = doScan(processDefinitionId, elementId);
                    currentMap.put(elementId, result);
                } catch (TransientException e) {
                    // transient failure — don't cache, retry next time
                }
            }
            return currentMap;
        });
        return Optional.ofNullable(resultMap.get(elementId));
    }

    public void unregisterAll() {
        cache.clear();
    }

    private AgentContextSpec doScan(String processDefinitionId, String elementId) throws TransientException {
        Optional<AgentConfig> config = agentConfigRegistry.resolve(processDefinitionId, elementId);
        if (config.isEmpty()) {
            return null;
        }

        try (InputStream xml = repositoryService.getObject().getProcessModel(processDefinitionId)) {
            if (xml == null) {
                LOG.warn("Process model not found for '{}'", processDefinitionId);
                return null;
            }

            Parse parse = new BpmnXmlParser().createParse().sourceInputStream(xml).execute();
            Element root = parse.getRootElement();

            String toolScopeElementId = config.get().toolScopeElementId();
            Element toolScopeElement = findElementById(root, toolScopeElementId);
            if (toolScopeElement == null) {
                LOG.warn("Tool scope element '{}' not found in process definition '{}'",
                        toolScopeElementId, processDefinitionId);
                return null;
            }

            return builder.build(toolScopeElement, processDefinitionId);
        } catch (IOException e) {
            LOG.error("Failed to scan process definition '{}' for '{}'",
                    processDefinitionId, this.getClass(), e);
            throw new TransientException(e);
        } catch (NotFoundException e) {
            LOG.error("Process definition '{}' not found", processDefinitionId, e);
            return null;
        } catch (AuthorizationException e) {
            LOG.error("Unauthorized process definition access attempt on '{}'", processDefinitionId, e);
            return null;
        }
    }

    private Element findElementById(Element element, String id) {
        if (id.equals(element.attribute("id")))
            return element;
        for (Element child : element.elements()) {
            Element found = findElementById(child, id);
            if (found != null)
                return found;
        }
        return null;
    }
}
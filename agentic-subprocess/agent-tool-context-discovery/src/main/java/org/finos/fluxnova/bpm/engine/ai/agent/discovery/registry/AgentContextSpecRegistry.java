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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central lookup for context specifications attached to BPMN scope elements.
 *
 * <p>Context specifications declare which process variables should be visible for a
 * given scope element. Results are cached per process definition on first access and
 * evicted on process undeployment.
 *
 * @see AgentContextSpec
 * @see AgentContextSpecBuilder
 */
public class AgentContextSpecRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AgentContextSpecRegistry.class);

    private final ConcurrentHashMap<String, HashMap<String, AgentContextSpec>> cache = new ConcurrentHashMap<>();

    private final AgentConfigRegistry agentConfigRegistry;
    private final AgentContextSpecBuilder builder;

    public AgentContextSpecRegistry(AgentConfigRegistry agentConfigRegistry,
                                    AgentContextSpecBuilder builder) {
        this.agentConfigRegistry = agentConfigRegistry;
        this.builder = builder;
    }

    /**
     * Resolves the context specification for a specific BPMN element within a process
     * definition. The result is cached after the first successful scan.
     *
     * <p>If the element carries no {@code <agent:context>} declaration, or if no
     * {@link org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig} is registered for
     * the element, an empty optional is returned. Transient failures (e.g. IO errors
     * reading the process model) are not cached, so a subsequent call may succeed.
     *
     * @param repositoryService   the service to resolve the configuration against
     * @param processDefinitionId the process definition to look up
     * @param elementId           the BPMN element whose context specification is requested
     * @return the context specification if one is declared for the element, otherwise empty
     */
    public Optional<AgentContextSpec> resolve(RepositoryService repositoryService, String processDefinitionId, String elementId) {
        HashMap<String, AgentContextSpec> resultMap = cache.compute(processDefinitionId, (key, value) -> {
            HashMap<String, AgentContextSpec> currentMap = value != null ? value : new HashMap<>();
            if (!currentMap.containsKey(elementId)) {
                try {
                    AgentContextSpec result = doScan(repositoryService, processDefinitionId, elementId);
                    currentMap.put(elementId, result);
                } catch (TransientException e) {
                    // transient failure — don't cache, retry next time
                }
            }
            return currentMap;
        });
        return Optional.ofNullable(resultMap.get(elementId));
    }

    /**
     * Clears all cached context specifications.
     *
     * <p>Typically invoked on process undeployment to ensure stale entries are not
     * served after redeployment.
     */
    public void unregisterAll() {
        cache.clear();
    }

    private AgentContextSpec doScan(RepositoryService repositoryService, String processDefinitionId, String elementId) throws TransientException {
        Optional<AgentConfig> config = agentConfigRegistry.resolve(repositoryService, processDefinitionId, elementId);
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
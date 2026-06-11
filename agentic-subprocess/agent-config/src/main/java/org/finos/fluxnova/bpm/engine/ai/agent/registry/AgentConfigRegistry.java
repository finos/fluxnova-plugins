package org.finos.fluxnova.bpm.engine.ai.agent.registry;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central lookup for agent configurations attached to BPMN elements.
 *
 * <p>Results are cached per process definition on first access and evicted on
 * process undeployment.
 *
 * @see AgentConfig
 */
public class AgentConfigRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AgentConfigRegistry.class);

    private final ConcurrentHashMap<String, HashMap<String, AgentConfig>> configCache = new ConcurrentHashMap<>();

    private final AgentConfigExtractor extractor;

    /**
     * Creates a new registry backed by the given repository service and extractor.
     *
     * @param extractor         strategy for parsing agent configuration from BPMN XML
     */
    public AgentConfigRegistry(AgentConfigExtractor extractor) {
        this.extractor = extractor;
    }

    /**
     * Resolves the agent configuration for a specific BPMN element within a process definition.
     *
     * @param repositoryService   the repository service used to fetch the BPMN model on a
     *                            cache miss; supplied per call so the registry holds no
     *                            reference to the process engine
     * @param processDefinitionId the process definition to look up
     * @param elementId           the BPMN element whose agent configuration is requested
     * @return the configuration if the element has one, otherwise empty
     * @throws NotFoundException      if the process definition does not exist
     * @throws AuthorizationException if the caller lacks access to the process definition
     */
    public Optional<AgentConfig> resolve(RepositoryService repositoryService, String processDefinitionId, String elementId) {
        HashMap<String, AgentConfig> definitionConfigs = configCache.computeIfAbsent(
                processDefinitionId, currId -> doScan(repositoryService, currId));
        return Optional.ofNullable(definitionConfigs.get(elementId));
    }

    /**
     * Clears all cached agent configurations.
     *
     * <p>Typically invoked on process undeployment to ensure stale entries are
     * not served after redeployment.
     */
    public void unregisterAll() {
        configCache.clear();
    }

    private HashMap<String, AgentConfig> doScan(RepositoryService repositoryService, String processDefinitionId) {
        InputStream xml;
        try {
            xml = repositoryService.getProcessModel(processDefinitionId);
        } catch (NotFoundException e) {
            LOG.error("Process definition '{}' not found", processDefinitionId, e);
            throw e;
        } catch (AuthorizationException e) {
            LOG.error("Unauthorized process definition access attempt on '{}'", processDefinitionId, e);
            throw e;
        }
        return extractor.extractAll(xml, processDefinitionId).stream()
                .collect(Collectors.toMap(AgentConfig::elementId, config -> config,
                        (a, b) -> b, HashMap::new));
    }
}

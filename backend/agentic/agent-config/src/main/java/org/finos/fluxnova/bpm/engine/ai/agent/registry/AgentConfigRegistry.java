package org.finos.fluxnova.bpm.engine.ai.agent.registry;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central lookup for agent configurations attached to BPMN elements.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * Optional<AgentConfig> config = registry.resolve(processDefinitionId, elementId);
 * }</pre>
 *
 * @see AgentConfig
 */
public class AgentConfigRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AgentConfigRegistry.class);

    private final ConcurrentHashMap<String, HashMap<String, AgentConfig>> configCache = new ConcurrentHashMap<>();

    private final ObjectProvider<RepositoryService> repositoryService;
    private final AgentConfigExtractor extractor;

    /**
     * Creates a new registry backed by the given repository service and extractor.
     *
     * @param repositoryService provider for the repository service, resolved lazily to avoid
     *                          circular dependencies during engine initialisation
     * @param extractor         strategy for parsing agent configuration from BPMN XML
     */
    public AgentConfigRegistry(ObjectProvider<RepositoryService> repositoryService, AgentConfigExtractor extractor) {
        this.repositoryService = repositoryService;
        this.extractor = extractor;
    }

    /**
     * Resolves the agent configuration for a specific BPMN element within a process definition.
     *
     * @param processDefinitionId the process definition to look up
     * @param elementId           the BPMN element whose agent configuration is requested
     * @return the configuration if the element has one, otherwise empty
     * @throws NotFoundException      if the process definition does not exist
     * @throws AuthorizationException if the caller lacks access to the process definition
     */
    public Optional<AgentConfig> resolve(String processDefinitionId, String elementId) {
        HashMap<String, AgentConfig> definitionConfigs = configCache.computeIfAbsent(
                processDefinitionId, this::doScan);
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

    private HashMap<String, AgentConfig> doScan(String processDefinitionId) {
        InputStream xml;
        try {
            xml = repositoryService.getObject().getProcessModel(processDefinitionId);
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
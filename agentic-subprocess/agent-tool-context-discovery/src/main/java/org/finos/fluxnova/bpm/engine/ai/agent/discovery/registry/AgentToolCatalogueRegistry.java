package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.exception.NotFoundException;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central lookup for tool catalogues associated with BPMN scope elements.
 *
 * <p>A tool catalogue lists the activities available as tools within a given scope.
 * Catalogues are built lazily on first access and cached per process definition.
 * Multiple scope elements pointing at the same {@code toolScopeElementId} share a
 * single catalogue instance. The cache is evicted on process undeployment.
 *
 * @see AgentToolCatalogue
 * @see AgentToolCatalogueBuilder
 */
public class AgentToolCatalogueRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AgentToolCatalogueRegistry.class);

    /**
     * Cache keyed by processDefinitionId → toolScopeElementId → catalogue.
     * Catalogues are cached by tool scope id so multiple agent configs pointing
     * at the same scope share one catalogue instance.
     */
    private final ConcurrentHashMap<String, HashMap<String, AgentToolCatalogue>> scopeCache = new ConcurrentHashMap<>();

    /**
     * Tracks which (processDefinitionId, agentElementId) pairs have already been resolved,
     * so config lookups are not repeated. Maps to the resolved toolScopeElementId (or null
     * if no config was found).
     */
    private final ConcurrentHashMap<String, HashMap<String, String>> resolvedAgents = new ConcurrentHashMap<>();

    private final AgentConfigRegistry agentConfigRegistry;
    private final AgentToolCatalogueBuilder catalogueBuilder;

    public AgentToolCatalogueRegistry(AgentConfigRegistry agentConfigRegistry,
                                      AgentToolCatalogueBuilder catalogueBuilder) {
        this.agentConfigRegistry = agentConfigRegistry;
        this.catalogueBuilder = catalogueBuilder;
    }

    /**
     * Resolves the tool catalogue for a BPMN element within a process definition.
     *
     * <p>The element's {@link org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig}
     * is consulted to determine the {@code toolScopeElementId}. The catalogue for that
     * scope is then built (if not already cached) using the configured
     * {@link AgentToolCatalogueBuilder}. Returns empty if no agent configuration exists
     * for the element, if the scope activity cannot be found, or if the process definition
     * is not accessible.
     *
     * @param repositoryService   the repository service used to fetch the process
     *                            definition and BPMN model on a cache miss; supplied per
     *                            call so the registry holds no reference to the engine
     * @param processDefinitionId the process definition to look up
     * @param agentElementId      the BPMN element id of the agent whose tool catalogue
     *                            is requested
     * @return the tool catalogue for the resolved scope, or empty if it could not be
     * determined
     */
    public Optional<AgentToolCatalogue> resolve(RepositoryService repositoryService, String processDefinitionId, String agentElementId) {
        // Check if this agent has already been resolved
        String toolScopeElementId = resolvedAgents.compute(processDefinitionId, (key, value) -> {
            HashMap<String, String> map = value != null ? value : new HashMap<>();
            if (!map.containsKey(agentElementId)) {
                Optional<AgentConfig> config = agentConfigRegistry.resolve(repositoryService, processDefinitionId, agentElementId);
                map.put(agentElementId, config.map(AgentConfig::toolScopeElementId).orElse(null));
            }
            return map;
        }).get(agentElementId);

        if (toolScopeElementId == null) {
            return Optional.empty();
        }

        // Look up or build the catalogue keyed by tool scope id
        HashMap<String, AgentToolCatalogue> resultMap = scopeCache.compute(processDefinitionId, (key, value) -> {
            HashMap<String, AgentToolCatalogue> currentMap = value != null ? value : new HashMap<>();
            if (!currentMap.containsKey(toolScopeElementId)) {
                AgentToolCatalogue result = doScan(repositoryService, processDefinitionId, toolScopeElementId);
                currentMap.put(toolScopeElementId, result);
            }
            return currentMap;
        });

        return Optional.ofNullable(resultMap.get(toolScopeElementId));
    }

    /**
     * Clears all cached tool catalogues.
     *
     * <p>Typically invoked on process undeployment to ensure stale entries are not
     * served after redeployment.
     */
    public void unregisterAll() {
        scopeCache.clear();
        resolvedAgents.clear();
    }

    /**
     * Invalidates all cached tool catalogues so they are rebuilt on next access,
     * while preserving the resolved agent → scope mappings (which depend only on the
     * static BPMN structure, not on remote agent metadata).
     *
     * <p>Intended to be called when upstream metadata that feeds catalogue construction
     * changes — for example when a remote agent's card is refreshed with new skills.
     * Because catalogues are resolved lazily at orchestration time, this ensures:
     * <ul>
     *   <li>process instances created after invalidation build a fresh catalogue;</li>
     *   <li>in-flight instances whose token has not yet reached the agentic subprocess
     *       also build a fresh catalogue when they arrive;</li>
     *   <li>instances already mid-orchestration are unaffected for their current turn
     *       (they already resolved and hold their catalogue).</li>
     * </ul>
     */
    public void invalidateAll() {
        LOG.info("Invalidating all cached tool catalogues; they will be rebuilt on next access");
        scopeCache.clear();
    }

    private AgentToolCatalogue doScan(RepositoryService repositoryService, String processDefinitionId, String toolScopeElementId) {
        try {
            ProcessDefinition processDefinition =
                    repositoryService.getProcessDefinition(processDefinitionId);

            if (!(processDefinition instanceof ProcessDefinitionEntity)) {
                LOG.warn("Process definition '{}' is not an instance of ProcessDefinitionEntity, cannot scan for tools",
                        processDefinitionId);
                return null;
            }

            ActivityImpl scope = ((ProcessDefinitionEntity) processDefinition).findActivity(toolScopeElementId);
            if (scope == null) {
                LOG.warn("Tool scope activity '{}' not found in process definition '{}'",
                        toolScopeElementId, processDefinitionId);
                return null;
            }

            return catalogueBuilder.build(scope);
        } catch (NotFoundException e) {
            LOG.error("Process definition '{}' not found", processDefinitionId, e);
            return null;
        } catch (AuthorizationException e) {
            LOG.error("Unauthorized process definition access attempt on '{}'", processDefinitionId, e);
            return null;
        }
    }
}
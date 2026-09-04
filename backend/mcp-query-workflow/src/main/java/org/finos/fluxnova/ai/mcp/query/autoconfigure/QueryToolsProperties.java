package org.finos.fluxnova.ai.mcp.query.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Configuration properties for controlling which MCP query tools are exposed.
 * <p>
 * Tools can be disabled at the service level (e.g. {@code fluxnova.mcp.query.tools.history.enabled=false})
 * or individual tools can be excluded by name (e.g. {@code fluxnova.mcp.query.tools.exclude=querySchemaLog,queryBatches}).
 */
@ConfigurationProperties(prefix = "fluxnova.mcp.query.tools")
public class QueryToolsProperties {

    private final ServiceToggle repository = new ServiceToggle();
    private final ServiceToggle runtime = new ServiceToggle();
    private final ServiceToggle task = new ServiceToggle();
    private final ServiceToggle history = new ServiceToggle();
    private final ServiceToggle externalTask = new ServiceToggle();
    private final ServiceToggle authorization = new ServiceToggle();
    private final ServiceToggle filter = new ServiceToggle();
    private final ServiceToggle caseService = new ServiceToggle();
    private final ServiceToggle identity = new ServiceToggle();
    private final ServiceToggle management = new ServiceToggle();
    private final ServiceToggle xml = new ServiceToggle();

    /**
     * Set of individual tool names to exclude from the MCP server.
     * Tool names correspond to the method names (e.g. "queryTasks", "queryHistoricBatches").
     */
    private Set<String> exclude = Set.of();

    public ServiceToggle getRepository() {
        return repository;
    }

    public ServiceToggle getRuntime() {
        return runtime;
    }

    public ServiceToggle getTask() {
        return task;
    }

    public ServiceToggle getHistory() {
        return history;
    }

    public ServiceToggle getExternalTask() {
        return externalTask;
    }

    public ServiceToggle getAuthorization() {
        return authorization;
    }

    public ServiceToggle getFilter() {
        return filter;
    }

    public ServiceToggle getCaseService() {
        return caseService;
    }

    public ServiceToggle getIdentity() {
        return identity;
    }

    public ServiceToggle getManagement() {
        return management;
    }

    public ServiceToggle getXml() {
        return xml;
    }

    public Set<String> getExclude() {
        return exclude;
    }

    public void setExclude(Set<String> exclude) {
        this.exclude = exclude;
    }

    public static class ServiceToggle {

        /**
         * Whether this service's tools are enabled. Defaults to true.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

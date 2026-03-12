package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.VariableInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/**
 * MCP tools for querying runtime process engine data.
 * <p>
 * Provides read-only query access to process instances, executions, incidents,
 * event subscriptions, and variable instances through the process engine's
 * RuntimeService Query API.
 */
public class RuntimeQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeQueryMcpTools.class);

    private final RuntimeService runtimeService;
    private final int defaultMaxResults;

    public RuntimeQueryMcpTools(RuntimeService runtimeService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.runtimeService = runtimeService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Process Instance Query ----

    @McpTool(description = "Query running process instances in the process engine. "
            + "Returns a list of process instances matching the given filter criteria. "
            + "Process instances represent individual executions of a process definition (workflow). "
            + "Use this tool to find active or suspended process instances by their definition, business key, "
            + "tenant, incident status, or other attributes. All filter parameters are optional.")
    public List<ProcessInstanceResultDto> queryProcessInstances(
            @McpToolParam ProcessInstanceQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying process instances with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<ProcessInstanceResultDto> resultDtos = queryDto.toQuery(runtimeService).list().stream()
                .limit(limit)
                .map(ProcessInstanceResultDto::fromProcessInstance)
                .toList();

        LOG.info("Process instance query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Execution Query ----

    @McpTool(description = "Query executions in the process engine. "
            + "Returns a list of executions matching the given filter criteria. "
            + "An execution represents a path of execution within a process instance - "
            + "a process instance is itself the root execution. Parallel gateways and "
            + "multi-instance activities create additional concurrent executions. "
            + "Use this tool to inspect execution state, find executions waiting for signals or messages, "
            + "or examine execution-level details. All filter parameters are optional.")
    public List<ExecutionResultDto> queryExecutions(
            @McpToolParam ExecutionQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying executions with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<ExecutionResultDto> resultDtos = queryDto.toQuery(runtimeService).list().stream()
                .limit(limit)
                .map(ExecutionResultDto::fromExecution)
                .toList();

        LOG.info("Execution query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Incident Query ----

    @McpTool(description = "Query incidents in the process engine. "
            + "Returns a list of incidents matching the given filter criteria. "
            + "Incidents represent problems that occurred during process execution, "
            + "such as failed jobs, failed external tasks, or other error conditions. "
            + "Use this tool to find and diagnose process execution failures. "
            + "All filter parameters are optional.")
    public List<IncidentResultDto> queryIncidents(
            @McpToolParam IncidentQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying incidents with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<IncidentResultDto> resultDtos = queryDto.toQuery(runtimeService).list().stream()
                .limit(limit)
                .map(IncidentResultDto::fromIncident)
                .toList();

        LOG.info("Incident query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Event Subscription Query ----

    @McpTool(description = "Query event subscriptions in the process engine. "
            + "Returns a list of event subscriptions matching the given filter criteria. "
            + "Event subscriptions represent points where a process instance is waiting for an external event, "
            + "such as a message event, signal event, compensation event, or conditional event. "
            + "Use this tool to find which process instances are waiting for specific events. "
            + "All filter parameters are optional.")
    public List<EventSubscriptionResultDto> queryEventSubscriptions(
            @McpToolParam EventSubscriptionQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying event subscriptions with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<EventSubscriptionResultDto> resultDtos = queryDto.toQuery(runtimeService).list().stream()
                .limit(limit)
                .map(EventSubscriptionResultDto::fromEventSubscription)
                .toList();

        LOG.info("Event subscription query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Variable Instance Query ----

    @McpTool(description = "Query variable instances in the process engine. "
            + "Returns a list of variable instances matching the given filter criteria. "
            + "Variables store data associated with process instances, executions, tasks, or case instances. "
            + "Each variable has a name, type, and value. Use this tool to inspect the current state of "
            + "process data across running or completed activities. All filter parameters are optional.")
    public List<VariableInstanceResultDto> queryVariableInstances(
            @McpToolParam VariableInstanceQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying variable instances with criteria: {}", queryDto);

        VariableInstanceQuery query = queryDto.toQuery(runtimeService);

        // Disable binary fetching by default to avoid loading large blobs
        query.disableBinaryFetching();

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<VariableInstanceResultDto> resultDtos = query.list().stream()
                .limit(limit)
                .map(VariableInstanceResultDto::fromVariableInstance)
                .toList();

        LOG.info("Variable instance query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.VariableInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

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
            int defaultMaxResults) {
        this.runtimeService = runtimeService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Process Instance Query ----
    public List<ProcessInstanceResultDto> queryProcessInstances(
            ProcessInstanceQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying process instances with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(runtimeService) : runtimeService.createProcessInstanceQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<ProcessInstanceResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(ProcessInstanceResultDto::fromProcessInstance)
                .toList();

        LOG.info("Process instance query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Execution Query ----
    public List<ExecutionResultDto> queryExecutions(
            ExecutionQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying executions with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(runtimeService) : runtimeService.createExecutionQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<ExecutionResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(ExecutionResultDto::fromExecution)
                .toList();

        LOG.info("Execution query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Incident Query ----
    public List<IncidentResultDto> queryIncidents(
            IncidentQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying incidents with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(runtimeService) : runtimeService.createIncidentQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<IncidentResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(IncidentResultDto::fromIncident)
                .toList();

        LOG.info("Incident query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Event Subscription Query ----
    public List<EventSubscriptionResultDto> queryEventSubscriptions(
            EventSubscriptionQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying event subscriptions with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(runtimeService) : runtimeService.createEventSubscriptionQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<EventSubscriptionResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(EventSubscriptionResultDto::fromEventSubscription)
                .toList();

        LOG.info("Event subscription query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Variable Instance Query ----
    public List<VariableInstanceResultDto> queryVariableInstances(
            VariableInstanceQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying variable instances with criteria: {}", queryDto);

        VariableInstanceQuery query = queryDto != null ? queryDto.toQuery(runtimeService) : runtimeService.createVariableInstanceQuery();

        // Disable binary fetching by default to avoid loading large blobs
        query.disableBinaryFetching();

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<VariableInstanceResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(VariableInstanceResultDto::fromVariableInstance)
                .toList();

        LOG.info("Variable instance query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

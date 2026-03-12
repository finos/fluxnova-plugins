package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.HistoryService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * MCP tools for querying historic data from the process engine via {@link HistoryService}.
 */
public class HistoryQueryMcpTools {

    private final HistoryService historyService;
    private final int defaultMaxResults;

    public HistoryQueryMcpTools(
            HistoryService historyService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.historyService = historyService;
        this.defaultMaxResults = defaultMaxResults;
    }

    @McpTool(name = "queryHistoricProcessInstances",
            description = "Query historic process instances using the criteria provided in the query DTO. "
                    + "Returns a list of historic process instances that match the specified filters.")
    public List<HistoricProcessInstanceResultDto> queryHistoricProcessInstances(
            @McpToolParam(description = "The query parameters for filtering historic process instances.",
                    required = false) HistoricProcessInstanceQueryDto queryDto) {
        var q = queryDto != null
                ? queryDto.toQuery(historyService)
                : historyService.createHistoricProcessInstanceQuery();
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricProcessInstanceResultDto::fromHistoricProcessInstance)
                .toList();
    }

    @McpTool(name = "queryHistoricActivityInstances",
            description = "Query historic activity instances using the criteria provided in the query DTO. "
                    + "Returns a list of historic activity instances that match the specified filters.")
    public List<HistoricActivityInstanceResultDto> queryHistoricActivityInstances(
            @McpToolParam(description = "The query parameters for filtering historic activity instances.",
                    required = false) HistoricActivityInstanceQueryDto queryDto) {
        var q = queryDto != null
                ? queryDto.toQuery(historyService)
                : historyService.createHistoricActivityInstanceQuery();
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricActivityInstanceResultDto::fromHistoricActivityInstance)
                .toList();
    }

    @McpTool(name = "queryHistoricTaskInstances",
            description = "Query historic task instances using the criteria provided in the query DTO. "
                    + "Returns a list of historic task instances that match the specified filters.")
    public List<HistoricTaskInstanceResultDto> queryHistoricTaskInstances(
            @McpToolParam(description = "The query parameters for filtering historic task instances.",
                    required = false) HistoricTaskInstanceQueryDto queryDto) {
        var q = queryDto != null
                ? queryDto.toQuery(historyService)
                : historyService.createHistoricTaskInstanceQuery();
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricTaskInstanceResultDto::fromHistoricTaskInstance)
                .toList();
    }

    @McpTool(name = "queryHistoricDetails",
            description = "Query historic details (variable updates, form fields, and form properties) "
                    + "using the criteria provided in the query DTO. "
                    + "Returns a list of historic details that match the specified filters.")
    public List<HistoricDetailResultDto> queryHistoricDetails(
            @McpToolParam(description = "The query parameters for filtering historic details.",
                    required = false) HistoricDetailQueryDto queryDto) {
        var q = queryDto != null
                ? queryDto.toQuery(historyService)
                : historyService.createHistoricDetailQuery();
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricDetailResultDto::fromHistoricDetail)
                .toList();
    }

    @McpTool(name = "queryHistoricVariableInstances",
            description = "Query historic variable instances using the criteria provided in the query DTO. "
                    + "Returns a list of historic variable instances that match the specified filters.")
    public List<HistoricVariableInstanceResultDto> queryHistoricVariableInstances(
            @McpToolParam(description = "The query parameters for filtering historic variable instances.",
                    required = false) HistoricVariableInstanceQueryDto queryDto) {
        var q = historyService.createHistoricVariableInstanceQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricVariableInstanceResultDto::fromHistoricVariableInstance)
                .toList();
    }

    @McpTool(name = "queryUserOperationLog",
            description = "Query user operation log entries using the criteria provided in the query DTO. "
                    + "Returns a list of user operation log entries that match the specified filters.")
    public List<UserOperationLogEntryResultDto> queryUserOperationLog(
            @McpToolParam(description = "The query parameters for filtering user operation log entries.",
                    required = false) UserOperationLogQueryDto queryDto) {
        var q = historyService.createUserOperationLogQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(UserOperationLogEntryResultDto::fromUserOperationLogEntry)
                .toList();
    }

    @McpTool(name = "queryHistoricIncidents",
            description = "Query historic incidents using the criteria provided in the query DTO. "
                    + "Returns a list of historic incidents that match the specified filters.")
    public List<HistoricIncidentResultDto> queryHistoricIncidents(
            @McpToolParam(description = "The query parameters for filtering historic incidents.",
                    required = false) HistoricIncidentQueryDto queryDto) {
        var q = historyService.createHistoricIncidentQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricIncidentResultDto::fromHistoricIncident)
                .toList();
    }

    @McpTool(name = "queryHistoricIdentityLinkLog",
            description = "Query historic identity link log entries using the criteria provided in the query DTO. "
                    + "Returns a list of historic identity link log entries that match the specified filters.")
    public List<HistoricIdentityLinkLogResultDto> queryHistoricIdentityLinkLog(
            @McpToolParam(description = "The query parameters for filtering historic identity link log entries.",
                    required = false) HistoricIdentityLinkLogQueryDto queryDto) {
        var q = historyService.createHistoricIdentityLinkLogQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricIdentityLinkLogResultDto::fromHistoricIdentityLinkLog)
                .toList();
    }

    @McpTool(name = "queryHistoricCaseInstances",
            description = "Query historic case instances using the criteria provided in the query DTO. "
                    + "Returns a list of historic case instances that match the specified filters.")
    public List<HistoricCaseInstanceResultDto> queryHistoricCaseInstances(
            @McpToolParam(description = "The query parameters for filtering historic case instances.",
                    required = false) HistoricCaseInstanceQueryDto queryDto) {
        var q = historyService.createHistoricCaseInstanceQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricCaseInstanceResultDto::fromHistoricCaseInstance)
                .toList();
    }

    @McpTool(name = "queryHistoricCaseActivityInstances",
            description = "Query historic case activity instances using the criteria provided in the query DTO. "
                    + "Returns a list of historic case activity instances that match the specified filters.")
    public List<HistoricCaseActivityInstanceResultDto> queryHistoricCaseActivityInstances(
            @McpToolParam(description = "The query parameters for filtering historic case activity instances.",
                    required = false) HistoricCaseActivityInstanceQueryDto queryDto) {
        var q = historyService.createHistoricCaseActivityInstanceQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricCaseActivityInstanceResultDto::fromHistoricCaseActivityInstance)
                .toList();
    }

    @McpTool(name = "queryHistoricDecisionInstances",
            description = "Query historic decision instances using the criteria provided in the query DTO. "
                    + "Returns a list of historic decision instances that match the specified filters.")
    public List<HistoricDecisionInstanceResultDto> queryHistoricDecisionInstances(
            @McpToolParam(description = "The query parameters for filtering historic decision instances.",
                    required = false) HistoricDecisionInstanceQueryDto queryDto) {
        var q = historyService.createHistoricDecisionInstanceQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricDecisionInstanceResultDto::fromHistoricDecisionInstance)
                .toList();
    }

    @McpTool(name = "queryHistoricJobLog",
            description = "Query historic job log entries using the criteria provided in the query DTO. "
                    + "Returns a list of historic job log entries that match the specified filters.")
    public List<HistoricJobLogResultDto> queryHistoricJobLog(
            @McpToolParam(description = "The query parameters for filtering historic job log entries.",
                    required = false) HistoricJobLogQueryDto queryDto) {
        var q = historyService.createHistoricJobLogQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricJobLogResultDto::fromHistoricJobLog)
                .toList();
    }

    @McpTool(name = "queryHistoricBatches",
            description = "Query historic batches using the criteria provided in the query DTO. "
                    + "Returns a list of historic batches that match the specified filters.")
    public List<HistoricBatchResultDto> queryHistoricBatches(
            @McpToolParam(description = "The query parameters for filtering historic batches.",
                    required = false) HistoricBatchQueryDto queryDto) {
        var q = historyService.createHistoricBatchQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricBatchResultDto::fromHistoricBatch)
                .toList();
    }

    @McpTool(name = "queryHistoricExternalTaskLog",
            description = "Query historic external task log entries using the criteria provided in the query DTO. "
                    + "Returns a list of historic external task log entries that match the specified filters.")
    public List<HistoricExternalTaskLogResultDto> queryHistoricExternalTaskLog(
            @McpToolParam(description = "The query parameters for filtering historic external task log entries.",
                    required = false) HistoricExternalTaskLogQueryDto queryDto) {
        var q = historyService.createHistoricExternalTaskLogQuery();
        if (queryDto != null) queryDto.toQuery(q);
        return q.listPage(0, defaultMaxResults).stream()
                .map(HistoricExternalTaskLogResultDto::fromHistoricExternalTaskLog)
                .toList();
    }
}

package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.HistoryService;

import java.util.List;

/**
 * MCP tools for querying historic data from the process engine via {@link HistoryService}.
 */
public class HistoryQueryMcpTools {

    private final HistoryService historyService;
    private final int defaultMaxResults;

    public HistoryQueryMcpTools(
            HistoryService historyService,
            int defaultMaxResults) {
        this.historyService = historyService;
        this.defaultMaxResults = defaultMaxResults;
    }
    public List<HistoricProcessInstanceResultDto> queryHistoricProcessInstances(
                    HistoricProcessInstanceQueryDto queryDto,
                    Integer maxResults) {
        var q = queryDto != null
                ? queryDto.toQuery(historyService)
                : historyService.createHistoricProcessInstanceQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricProcessInstanceResultDto::fromHistoricProcessInstance)
                .toList();
    }
    public List<HistoricActivityInstanceResultDto> queryHistoricActivityInstances(
                    HistoricActivityInstanceQueryDto queryDto,
                    Integer maxResults) {
        var q = queryDto != null
                ? queryDto.toQuery(historyService)
                : historyService.createHistoricActivityInstanceQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricActivityInstanceResultDto::fromHistoricActivityInstance)
                .toList();
    }
    public List<HistoricTaskInstanceResultDto> queryHistoricTaskInstances(
                    HistoricTaskInstanceQueryDto queryDto,
                    Integer maxResults) {
        var q = queryDto != null
                ? queryDto.toQuery(historyService)
                : historyService.createHistoricTaskInstanceQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricTaskInstanceResultDto::fromHistoricTaskInstance)
                .toList();
    }
    public List<HistoricDetailResultDto> queryHistoricDetails(
                    HistoricDetailQueryDto queryDto,
                    Integer maxResults) {
        var q = queryDto != null
                ? queryDto.toQuery(historyService)
                : historyService.createHistoricDetailQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricDetailResultDto::fromHistoricDetail)
                .toList();
    }
    public List<HistoricVariableInstanceResultDto> queryHistoricVariableInstances(
                    HistoricVariableInstanceQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricVariableInstanceQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricVariableInstanceResultDto::fromHistoricVariableInstance)
                .toList();
    }
    public List<UserOperationLogEntryResultDto> queryUserOperationLog(
                    UserOperationLogQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createUserOperationLogQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(UserOperationLogEntryResultDto::fromUserOperationLogEntry)
                .toList();
    }
    public List<HistoricIncidentResultDto> queryHistoricIncidents(
                    HistoricIncidentQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricIncidentQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricIncidentResultDto::fromHistoricIncident)
                .toList();
    }
    public List<HistoricIdentityLinkLogResultDto> queryHistoricIdentityLinkLog(
                    HistoricIdentityLinkLogQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricIdentityLinkLogQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricIdentityLinkLogResultDto::fromHistoricIdentityLinkLog)
                .toList();
    }
    public List<HistoricCaseInstanceResultDto> queryHistoricCaseInstances(
                    HistoricCaseInstanceQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricCaseInstanceQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricCaseInstanceResultDto::fromHistoricCaseInstance)
                .toList();
    }
    public List<HistoricCaseActivityInstanceResultDto> queryHistoricCaseActivityInstances(
                    HistoricCaseActivityInstanceQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricCaseActivityInstanceQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricCaseActivityInstanceResultDto::fromHistoricCaseActivityInstance)
                .toList();
    }
    public List<HistoricDecisionInstanceResultDto> queryHistoricDecisionInstances(
                    HistoricDecisionInstanceQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricDecisionInstanceQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricDecisionInstanceResultDto::fromHistoricDecisionInstance)
                .toList();
    }
    public List<HistoricJobLogResultDto> queryHistoricJobLog(
                    HistoricJobLogQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricJobLogQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricJobLogResultDto::fromHistoricJobLog)
                .toList();
    }
    public List<HistoricBatchResultDto> queryHistoricBatches(
                    HistoricBatchQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricBatchQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricBatchResultDto::fromHistoricBatch)
                .toList();
    }
    public List<HistoricExternalTaskLogResultDto> queryHistoricExternalTaskLog(
                    HistoricExternalTaskLogQueryDto queryDto,
                    Integer maxResults) {
        var q = historyService.createHistoricExternalTaskLogQuery();
        if (queryDto != null) queryDto.toQuery(q);
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        return q.listPage(0, limit).stream()
                .map(HistoricExternalTaskLogResultDto::fromHistoricExternalTaskLog)
                .toList();
    }
}

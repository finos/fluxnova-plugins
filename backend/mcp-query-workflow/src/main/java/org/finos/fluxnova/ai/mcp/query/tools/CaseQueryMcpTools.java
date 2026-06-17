package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.CaseExecutionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.CaseInstanceResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.CaseExecutionQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.CaseInstanceQueryDto;
import org.finos.fluxnova.bpm.engine.CaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * MCP tools for querying case instance and case execution data from the process engine.
 * <p>
 * Provides read-only query access to CMMN case instances and case executions
 * through the process engine's CaseService Query API.
 */
public class CaseQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(CaseQueryMcpTools.class);

    private final CaseService caseService;
    private final int defaultMaxResults;

    public CaseQueryMcpTools(CaseService caseService,
                             int defaultMaxResults) {
        this.caseService = caseService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Case Instance Query ----
    public List<CaseInstanceResultDto> queryCaseInstances(
            CaseInstanceQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying case instances with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(caseService) : caseService.createCaseInstanceQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<CaseInstanceResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(CaseInstanceResultDto::fromCaseInstance)
                .toList();

        LOG.info("Case instance query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Case Execution Query ----
    public List<CaseExecutionResultDto> queryCaseExecutions(
            CaseExecutionQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying case executions with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(caseService) : caseService.createCaseExecutionQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<CaseExecutionResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(CaseExecutionResultDto::fromCaseExecution)
                .toList();

        LOG.info("Case execution query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

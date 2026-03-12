package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.CaseExecutionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.CaseInstanceResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.CaseExecutionQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.CaseInstanceQueryDto;
import org.finos.fluxnova.bpm.engine.CaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;

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
                             @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.caseService = caseService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Case Instance Query ----

    @McpTool(description = "Query CMMN case instances in the process engine. "
            + "A case instance is the running execution of a CMMN case definition. "
            + "Use this tool to find case instances by id, business key, case definition, "
            + "lifecycle state (active, completed, terminated), super/sub process or case linkage, "
            + "and tenant. All filter parameters are optional.")
    public List<CaseInstanceResultDto> queryCaseInstances(
            @McpToolParam CaseInstanceQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying case instances with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<CaseInstanceResultDto> resultDtos = queryDto.toQuery(caseService).list().stream()
                .limit(limit)
                .map(CaseInstanceResultDto::fromCaseInstance)
                .toList();

        LOG.info("Case instance query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Case Execution Query ----

    @McpTool(description = "Query CMMN case executions in the process engine. "
            + "A case execution represents a planned item (stage, milestone, human task, or process task) "
            + "within a running case instance. "
            + "Use this tool to find executions by id, case instance, case definition, activity, "
            + "or lifecycle state (available, enabled, active, disabled, required). "
            + "All filter parameters are optional.")
    public List<CaseExecutionResultDto> queryCaseExecutions(
            @McpToolParam CaseExecutionQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying case executions with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<CaseExecutionResultDto> resultDtos = queryDto.toQuery(caseService).list().stream()
                .limit(limit)
                .map(CaseExecutionResultDto::fromCaseExecution)
                .toList();

        LOG.info("Case execution query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

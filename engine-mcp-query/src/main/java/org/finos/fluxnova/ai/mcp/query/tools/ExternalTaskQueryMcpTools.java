package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.ExternalTaskResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.ExternalTaskQueryDto;
import org.finos.fluxnova.bpm.engine.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * MCP tools for querying external task data from the process engine.
 * <p>
 * Provides read-only query access to external tasks
 * through the process engine's ExternalTaskService Query API.
 */
public class ExternalTaskQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalTaskQueryMcpTools.class);

    private final ExternalTaskService externalTaskService;
    private final int defaultMaxResults;

    public ExternalTaskQueryMcpTools(ExternalTaskService externalTaskService,
                                     @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.externalTaskService = externalTaskService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- External Task Query ----

    @McpTool(description = "Query external tasks in the process engine. "
            + "Returns a list of external tasks matching the given filter criteria. "
            + "An external task is created when a service-task-like activity is configured with "
            + "the external task pattern (camunda:type=\"external\"). "
            + "The task is placed on a topic and picked up by an external worker that completes it. "
            + "Use this tool to find pending, locked, or failed external tasks by topic, worker, "
            + "process instance, priority, or retry status. All filter parameters are optional.")
    public List<ExternalTaskResultDto> queryExternalTasks(
            @McpToolParam ExternalTaskQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying external tasks with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<ExternalTaskResultDto> resultDtos = queryDto.toQuery(externalTaskService).list().stream()
                .limit(limit)
                .map(ExternalTaskResultDto::fromExternalTask)
                .toList();

        LOG.info("External task query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

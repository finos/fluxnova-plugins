package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/**
 * MCP tools for querying task data from the process engine.
 * <p>
 * Provides read-only query access to user tasks
 * through the process engine's TaskService Query API.
 */
public class TaskQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(TaskQueryMcpTools.class);

    private final TaskService taskService;
    private final int defaultMaxResults;

    public TaskQueryMcpTools(TaskService taskService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.taskService = taskService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Task Query ----

    @McpTool(description = "Query user tasks in the process engine. "
            + "Returns a list of tasks matching the given filter criteria. "
            + "A task represents a piece of work that needs to be done by a human user, "
            + "typically a user task in a BPMN process or a human task in a CMMN case. "
            + "Use this tool to find tasks assigned to or available for a specific user or group, "
            + "filter by process or case context, priority, due dates, follow-up dates, "
            + "delegation state, or other task attributes. All filter parameters are optional.")
    public List<TaskResultDto> queryTasks(
            @McpToolParam TaskQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying tasks with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<TaskResultDto> resultDtos = queryDto.toQuery(taskService).list().stream()
                .limit(limit)
                .map(TaskResultDto::fromTask)
                .toList();

        LOG.info("Task query returned {} results", resultDtos.size());
        return resultDtos;
    }
}

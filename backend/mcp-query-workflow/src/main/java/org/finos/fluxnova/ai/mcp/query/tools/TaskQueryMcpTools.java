package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

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
            int defaultMaxResults) {
        this.taskService = taskService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Task Query ----
    public List<TaskResultDto> queryTasks(
            TaskQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying tasks with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(taskService) : taskService.createTaskQuery().initializeFormKeys();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<TaskResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(TaskResultDto::fromTask)
                .toList();

        LOG.info("Task query returned {} results", resultDtos.size());
        return resultDtos;
    }
}
